//use always the latest version, which contains latest bugfixes
pragma solidity ^0.4.24;

import "./SafeMath.sol";
import "./SafeMath192.sol";
import "./Utils.sol";

contract ERC20 {
    function allowance(address owner, address spender) public view returns (uint256);
    function transferFrom(address from, address to, uint256 value) public returns (bool);
    function approve(address spender, uint256 value) public returns (bool);
    function totalSupply() public view returns (uint256);
    function balanceOf(address who) public view returns (uint256);
    function transfer(address to, uint256 value) public returns (bool);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event Transfer(address indexed from, address indexed to, uint256 value);
}

/**
 * @title ERC677 transferAndCall token interface
 * @dev See https://github.com/ethereum/EIPs/issues/677 for specification and
 *      discussion.
 *
 * We deviate from the specification and we don't define a tokenfallback. That means
 * tranferAndCall can specify the function to call (bytes4(sha3("setN(uint256)")))
 * and its arguments, and the respective function is called.
 * TODO: find out what happens if the function is not found. Will the default function
 * be called, or will the function return false?
 *
 * We also deviate from ERC865 and added a pre signed transaction for transferAndCall.
 */
contract ERC865Plus677ish {
    event Transfer(address indexed _from, address indexed _to, uint256 _value, bytes4 _methodName, bytes _args);
    function transferAndCall(address _to, uint256 _value, bytes4 _methodName, bytes _args) public returns (bool success);

    event TransferPreSigned(address indexed _from, address indexed _to, address indexed _delegate,
        uint256 _amount, uint256 _fee);
    event TransferPreSigned(address indexed _from, address indexed _to, address indexed _delegate,
        uint256 _amount, uint256 _fee, bytes4 _methodName, bytes _args);

    function transferPreSigned(bytes _signature, address _to, uint256 _value,
        uint256 _fee, uint256 _nonce) public returns (bool);
    function transferAndCallPreSigned(bytes _signature, address _to, uint256 _value,
        uint256 _fee, uint256 _nonce, bytes4 _methodName, bytes _args) public returns (bool);
}

//TODO: check ERC865 for latest development
contract DOS is ERC20, ERC865Plus677ish {

    using SafeMath for uint256;
    using SafeMath192 for uint192;

    string public constant name = "DOS Token";
    string public constant symbol = "DOS";
    uint8 public constant decimals = 18;

    mapping(address => Snapshot[]) balances;
    mapping(address => mapping(address => uint256)) internal allowed;
    // nonces of transfers performed
    mapping(bytes => bool) signatures;

    uint256 public totalSupply_;
    //according to https://dezos.io/
    //this will fit int 2^90:
    //1237940039285380274899124224 (2^90)
    // 900000000000000000000000000 (max supply)
    uint256 constant public maxSupply = 900000000 * (10 ** uint256(decimals));

    //we want to create a snapshot of the token balances will fit into 256bit
    struct Snapshot {
        // fromBlock is the block number that the value was generated from
        uint64 fromBlock;
        uint192 amount;
    }

    // token lockups
    mapping(address => uint256) lockups;

    // ownership
    address public owner;

    // minting
    bool public mintingDone = false;

    event TokensLocked(address indexed _holder, uint256 _timeout);

    constructor() public {
        owner = msg.sender;
    }

    /**
     * @dev Allows the current owner to transfer the ownership.
     * @param _newOwner The address to transfer ownership to.
     */
    function transferOwnership(address _newOwner) public {
        require(owner == msg.sender);
        owner = _newOwner;
    }

    // minting functionality
    function mint(address[] _recipients, uint192[] _amounts) public {
        require(owner == msg.sender);
        require(mintingDone == false);
        require(_recipients.length == _amounts.length);
        require(_recipients.length <= 256);

        for (uint8 i = 0; i < _recipients.length; i++) {
            address recipient = _recipients[i];
            uint192 amount = _amounts[i];

            if(balances[recipient].length == 0) {
                Snapshot memory tmp;
                tmp.fromBlock = uint64(block.number);
                tmp.amount = amount;
                balances[recipient].push(tmp);
            } else {
                Snapshot storage current = balances[recipient][balances[recipient].length - 1];
                current.amount = current.amount.add(amount);
            }

            totalSupply_ = totalSupply_.add(uint256(amount));
            require(totalSupply_ <= maxSupply); // enforce maximum token supply

            emit Transfer(0, recipient, uint256(amount));
        }
    }

    function lockTokens(address[] _holders, uint256[] _timeouts) public {
        require(owner == msg.sender);
        require(mintingDone == false);
        require(_holders.length == _timeouts.length);
        require(_holders.length <= 256);

        for (uint8 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 timeout = _timeouts[i];

            // make sure lockup period can not be overwritten
            require(lockups[holder] == 0);

            lockups[holder] = timeout;
            emit TokensLocked(holder, timeout);
        }
    }

    //If this is called, no more tokens can be generated
    //The status of the contract can be checked with getMintingDone, as
    //this variable is set to public, thus, getters are generated automatically
    function finishMinting() public {
        require(owner == msg.sender);
        require(mintingDone == false);

        mintingDone = true;
    }

    /**
    * @dev total number of tokens in existence, which is mandated by the ERC20 interface
    */
    function totalSupply() public view returns (uint256) {
        return totalSupply_;
    }

    function transfer(address _to, uint256 _value) public returns (bool) {
        doTransfer(msg.sender, _to, _value, 0);
        emit Transfer(msg.sender, _to, _value); 
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public returns (bool) {
        require(_value <= allowed[_from][msg.sender]);
        doTransfer(_from, _to, _value, 0);
        allowed[_from][msg.sender] = allowed[_from][msg.sender].sub(_value);
        emit Transfer(_from, _to, _value);
        return true;
    }

    function doTransfer(address _from, address _to, uint256 _value, uint256 _fee, address _feeAddress) internal {
        require(_to != address(0));
        uint256 fromValue = balanceOf(_from);
        uint256 total = _value.add(_fee);
        require(total <= fromValue);
        require(mintingDone == true);

        // check lockups
        if (lockups[_from] != 0) {
            require(now >= lockups[_from]);
        }

        //TODO: convert from 256 to 192
        from(fromValue, total, _from);
        uint256 feeBalance = balanceOf(_feeAddress);
        fee(feeBalance, _fee, _feeAddress); //event is TransferPreSigned, that will be emitted after this function call
        uint256 toValue = balanceOf(_to);
        to(toValue, _value, _to);
    }

    function from(uint192 _fromBalance, uint192 _totalValue, address _fromAddress) internal {
        SnapshotAmount memory tmpFrom;
        tmpFrom.fromBlock = uint64(block.number);
        tmpFrom.amount = _fromBalance.sub(_totalValue);
        balances[_fromAddress].push(tmpFrom);
    }

    function fee(uint192 _fromFee, uint192 _fee, address _feeAddress) internal {
        if(_fee > 0 && _feeAddress != address(0)) {
            SnapshotAmount memory tmpFee;
            tmpFee.fromBlock = uint64(block.number);
            tmpFee.amount =  _fromFee.add(_fee);
            balances[_feeAddress].amounts.push(tmpFee);
        }
    }

    function to(uint192 _toBalance, uint192 _totalValue, address _toAddress) internal {
        SnapshotAmount memory tmpTo;
        tmpTo.fromBlock = uint64(block.number);
        tmpTo.amount = _toBalance.add(_totalValue);
        balances[_toAddress].push(tmpTo);
    }

    /**
    * @dev Gets the balance of the specified address.
    * @param _owner The address to query the the balance of.
    * @return An uint256 representing the amount owned by the passed address.
    */
    function balanceOf(address _owner) public view returns (uint256) {
        if(balances[_owner].length == 0) {
            return 0;
        }
        //return last amount
        return balances[_owner][balances[_owner].length - 1].amount;
    }

    //This is used to get the balance from a specific point in the history. This is the only way,
    //how a historic balance can be used within solidity contracts
    function balanceOf(address _owner, uint96 _fromBlock) public view returns (uint256) {
        // Binary search of the value in the array
        uint min = 0;
        uint max = balances[_owner].length-1;
        while (max > min) {
            uint mid = (max + min + 1)/ 2;
            if (balances[_owner][mid].fromBlock<=_fromBlock) {
                min = mid;
            } else {
                max = mid-1;
            }
        }

        return balances[_owner][min].amount;
    }



    /**
     * @dev Approve the passed address to spend the specified amount of tokens on behalf of msg.sender.
     *
     * Beware that changing an allowance with this method brings the risk that someone may use both the old
     * and the new allowance by unfortunate transaction ordering. One possible solution to mitigate this
     * race condition is to first reduce the spender's allowance to 0 and set the desired value afterwards:
     * https://github.com/ethereum/EIPs/issues/20#issuecomment-263524729
     * @param _spender The address which will spend the funds.
     * @param _value The amount of tokens to be spent.
     */
    function approve(address _spender, uint256 _value) public returns (bool) { //???do we need this proxy? 
        require(mintingDone == true);
        allowed[msg.sender][_spender] = _value;
        emit Approval(msg.sender, _spender, _value);
        return true;
    }

    /**
     * @dev Function to check the amount of tokens that an owner allowed to a spender.
     * @param _owner address The address which owns the funds.
     * @param _spender address The address which will spend the funds.
     * @return A uint256 specifying the amount of tokens still available for the spender.
     */
    function allowance(address _owner, address _spender) public view returns (uint256) {
        return allowed[_owner][_spender];
    }

    /**
     * @dev Increase the amount of tokens that an owner allowed to a spender.
     *
     * approve should be called when allowed[_spender] == 0. To increment
     * allowed value is better to use this function to avoid 2 calls (and wait until
     * the first transaction is mined)
     * From MonolithDAO Token.sol
     * @param _spender The address which will spend the funds.
     * @param _addedValue The amount of tokens to increase the allowance by.
     */
    function increaseApproval(address _spender, uint _addedValue) public returns (bool) {
        require(mintingDone == true);

        allowed[msg.sender][_spender] = allowed[msg.sender][_spender].add(_addedValue);
        emit Approval(msg.sender, _spender, allowed[msg.sender][_spender]);
        return true;
    }

    /**
     * @dev Decrease the amount of tokens that an owner allowed to a spender.
     *
     * approve should be called when allowed[_spender] == 0. To decrement
     * allowed value is better to use this function to avoid 2 calls (and wait until
     * the first transaction is mined)
     * From MonolithDAO Token.sol
     * @param _spender The address which will spend the funds.
     * @param _subtractedValue The amount of tokens to decrease the allowance by.
     */
    function decreaseApproval(address _spender, uint _subtractedValue) public returns (bool) {
        require(mintingDone == true);

        uint oldValue = allowed[msg.sender][_spender];
        if (_subtractedValue > oldValue) {
            allowed[msg.sender][_spender] = 0;
        } else {
            allowed[msg.sender][_spender] = oldValue.sub(_subtractedValue);
        }
        emit Approval(msg.sender, _spender, allowed[msg.sender][_spender]);
        return true;
    }

    function transferAndCall(address _to, uint _value, bytes _data) public returns (bool) {//???redundant= calls are mode from one contract to other internally, might not be security threat but these overheads can cost more than calling the function directly.
        return transferAndCall(_to, _value, _data, 0);
    }

    // ERC677 functionality
    function transferAndCall(address _to, uint _value, bytes _data, uint8 _fromType) public returns (bool) {
        require(mintingDone == true);
        require(transfer(_to, _value));

        emit Transfer(msg.sender, _to, _value, _data);

        // call receiver
        if (Utils.isContract(_to)) { //??? external contract call can be problematic so we can mark it external
            ERC677Receiver receiver = ERC677Receiver(_to);
            receiver.tokenFallback(msg.sender, _value, _data);
        }
        return true;
    }

    //ERC 865 + delegate transfer and call

    function transferPreSigned(bytes _signature, address _to, uint256 _value, uint256 _fee, //???redundant= calls are mode from one contract to other internally, might not be security threat but these overheads can cost more than calling the function directly.
        uint256 _nonce) public returns (bool) {
        return transferPreSigned(_signature, _to, _value, _fee, _nonce, 0);//???is the nounce meaning memo/description?
    }

    function transferPreSigned(bytes _signature, address _to, uint256 _value, uint256 _fee,
        uint256 _nonce, uint8 _fromType) public returns (bool) {

        require(signatures[_signature] == false);

        bytes32 hashedTx = Utils.transferPreSignedHashing(address(this), _to, _value, _fee, _nonce);//???external contract call
        address from = Utils.recover(hashedTx, _signature);//???external contract call
        require(from != address(0));

        doTransfer(from, _to, _value, _fee, _fromType);
        balances[msg.sender][balances[msg.sender].length -1].amount[0] = balanceOf(msg.sender).add(_fee);
        signatures[_signature] = true;

        emit Transfer(from, _to, _value);
        emit Transfer(from, msg.sender, _fee);
        emit TransferPreSigned(from, _to, msg.sender, _value, _fee);
        return true;
    }

    function transferAndCallPreSigned(bytes _signature, address _to, uint256 _value, uint256 _fee, uint256 _nonce, //???redundant
        bytes _data) public returns (bool) { //???is this gonna be used for messaging? what is _data? the two functions should have separate or non ambigous names
        return transferAndCallPreSigned(_signature, _to, _value, _fee, _nonce, _data, 0);
    }

    function transferAndCallPreSigned(bytes _signature, address _to, uint256 _value, uint256 _fee, uint256 _nonce,
        bytes _data, uint8 _fromType) public returns (bool) {

        require(signatures[_signature] == false);

        bytes32 hashedTx = Utils.transferPreSignedHashing(address(this), _to, _value, _fee, _nonce, _data);
        address from = Utils.recover(hashedTx, _signature);
        require(from != address(0));

        doTransfer(from, _to, _value, _fee, _fromType);
        balances[msg.sender][balances[msg.sender].length -1].amount[0] = balanceOf(msg.sender).add(_fee);
        signatures[_signature] = true;

        emit Transfer(from, _to, _value);
        emit Transfer(from, msg.sender, _fee);
        emit TransferPreSigned(from, _to, msg.sender, _value, _fee, _data);

        // call receiver
        if (Utils.isContract(_to)) {
            ERC677Receiver receiver = ERC677Receiver(_to);
            receiver.tokenFallback(from, _value, _data);
        }
        return true;
    }

    function createCurrentSnapshotForAddress(address addr) internal returns (Snapshot){
        //check memory vs. storage options
        Snapshot memory tmp;
        tmp.fromAddress = msg.sender;
        tmp.fromBlock = uint64(block.number);
        balances[addr].push(tmp);
    }
}
