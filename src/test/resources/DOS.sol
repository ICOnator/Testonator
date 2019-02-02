pragma solidity ^0.5.2;

import "./SafeMath.sol";
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
 *
 * If an invalid function is called, its default function (if implemented) is called.
 *
 * We also deviate from ERC865 and added a pre signed transaction for transferAndCall.
 */

/*
 Notes on signature malleability: Ethereum took the same
 precaution as in bitcoin was used to prevent that:

 https://github.com/ethereum/go-ethereum/blob/master/vendor/github.com/btcsuite/btcd/btcec/signature.go#L48
 https://github.com/ethereum/go-ethereum/blob/master/crypto/signature_test.go
 https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md
 https://github.com/ethereum/EIPs/blob/master/EIPS/eip-2.md

 However, ecrecover still allows ambigous signatures. Thus, recover that wraps ecrecover checks for ambigous
 signatures and only allows unique signatures.
*/

contract ERC865Plus677ish {
    event TransferAndCall(address indexed _from, address indexed _to, uint256 _value, bytes4 _methodName, bytes _args);
    function transferAndCall(address _to, uint256 _value, bytes4 _methodName, bytes memory _args) public returns (bytes memory);

    event TransferPreSigned(address indexed _from, address indexed _to, address indexed _delegate,
        uint256 _amount, uint256 _fee);
    event TransferAndCallPreSigned(address indexed _from, address indexed _to, address indexed _delegate,
        uint256 _amount, uint256 _fee, bytes4 _methodName, bytes _args);

    function transferPreSigned(bytes memory _signature, address _to, uint256 _value,
        uint256 _fee, uint256 _nonce) public returns (bool);
    function transferAndCallPreSigned(bytes memory _signature, address _to, uint256 _value,
        uint256 _fee, uint256 _nonce, bytes4 _methodName, bytes memory _args) public returns (bytes memory);
}

contract DOS is ERC20, ERC865Plus677ish {
    using SafeMath for uint256;

    string public constant name = "DOS Token";
    string public constant symbol = "DOS";
    uint8 public constant decimals = 18;

    mapping(address => uint256) balances;
    mapping(address => mapping(address => uint256)) internal allowed;
    // nonces of transfers performed
    mapping(bytes => bool) signatures;

    uint256 public totalSupply_;
    uint256 public constant maxSupply = 900000000 * (10 ** uint256(decimals));

    // token lockups
    mapping(address => uint256) lockups;

    // ownership
    address public owner;
    address public admin1;
    address public admin2;

    //3 admins can disable the transfers, however, the balances remain.
    //this can be used to migrate to another contract. This flag can only
    //be set by 3 admins.
    bool public transfersEnabled1 = true;
    bool public transfersEnabled2 = true;
    bool public transfersEnabled3 = true;

    // minting
    bool public mintingDone = false;

    //vesting variables, check dates with https://www.epochconverter.com/ and https://www.unixtimestamp.com/
    uint256 public constant firstFeb19 = 1548979200;
    uint256 public constant sixMonth = 6 * 30 days;

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

    function setAdmin(address _admin1, address _admin2) public {
        require(owner == msg.sender);
        require(!mintingDone);
        admin1 = _admin1;
        admin2 = _admin2;
    }

    // minting functionality
    function mint(address[] memory _recipients, uint256[] memory _amounts) public {
        require(owner == msg.sender);
        require(!mintingDone);
        require(_recipients.length == _amounts.length);
        require(_recipients.length <= 256);

        for (uint8 i = 0; i < _recipients.length; i++) {
            address recipient = _recipients[i];
            uint256 amount = _amounts[i];

            balances[recipient] += amount;

            totalSupply_ = totalSupply_.add(amount);
            require(totalSupply_ <= maxSupply); // enforce maximum token supply

            emit Transfer(address(0), recipient, amount);
        }
    }

    /**
     * @param _sixMonthCliff Number of a six month cliff. E.g., 1 is for 6 month, 2 is for 12 month, 3 is for 18 month, etc.
     */
    function lockTokens(address[] memory _holders, uint256[] memory _sixMonthCliff) public {
        require(owner == msg.sender);
        require(!mintingDone);
        require(_holders.length == _sixMonthCliff.length);
        require(_holders.length <= 256);

        for (uint8 i = 0; i < _holders.length; i++) {
            address holder = _holders[i];
            uint256 timeout = (_sixMonthCliff[i].mul(sixMonth)).add(firstFeb19);

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
        require(!mintingDone);
        require(admin1 != address(0));
        require(admin2 != address(0));

        mintingDone = true;
    }

    function transferDisable() public {
        if(msg.sender == owner) {
            transfersEnabled1 = false;
        } else if(msg.sender == admin1) {
            transfersEnabled2 = false;
        } else if(msg.sender == admin2) {
            transfersEnabled3 = false;
        } else {
            revert();
        }
    }

    function isTransferEnabled() public view returns (bool) {
        //all three must agree to disable the transfer
        return transfersEnabled1 || transfersEnabled2 || transfersEnabled3;
    }

    /**
    * @dev total number of tokens in existence, which is mandated by the ERC20 interface
    */
    function totalSupply() public view returns (uint256) {
        return totalSupply_;
    }

    function transfer(address _to, uint256 _value) public returns (bool) {
        doTransfer(msg.sender, _to, _value, 0, address(0));
        emit Transfer(msg.sender, _to, _value);
        return true;
    }

    function transferFrom(address _from, address _to, uint256 _value) public returns (bool) {
        allowed[_from][msg.sender] = allowed[_from][msg.sender].sub(_value);
        doTransfer(_from, _to, _value, 0, address(0));
        emit Transfer(_from, _to, _value);
        return true;
    }

    function doTransfer(address _from, address _to, uint256 _value, uint256 _fee, address _feeAddress) internal {
        require(isTransferEnabled());
        require(_to != address(0));
        uint256 total = _value.add(_fee);
        require(mintingDone);
        require(now >= lockups[_from]); // check lockups
        require(total <= balances[_from]);

        balances[_from] = balances[_from].sub(total);

        if(_fee > 0 && _feeAddress != address(0)) {
            balances[_feeAddress] = balances[_feeAddress].add(_fee);
        }

        balances[_to] = balances[_to].add(_value);
    }

    /**
    * @dev Gets the balance of the specified address.
    * @param _owner The address to query the the balance of.
    * @return An uint256 representing the amount owned by the passed address.
    */
    function balanceOf(address _owner) public view returns (uint256) {
        return balances[_owner];
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
    function approve(address _spender, uint256 _value) public returns (bool) {
        require(_spender != address(0));
        require(mintingDone);
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
    function increaseApproval(address _spender, uint256 _addedValue) public returns (bool) {
        require(mintingDone);

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
    function decreaseApproval(address _spender, uint256 _subtractedValue) public returns (bool) {
        require(mintingDone);

        uint oldValue = allowed[msg.sender][_spender];
        if (_subtractedValue > oldValue) {
            allowed[msg.sender][_spender] = 0;
        } else {
            allowed[msg.sender][_spender] = oldValue.sub(_subtractedValue);
        }
        emit Approval(msg.sender, _spender, allowed[msg.sender][_spender]);
        return true;
    }

    function transferAndCall(address _to, uint256 _value, bytes4 _methodName, bytes memory _args) public returns (bytes memory) {
        require(transfer(_to, _value));

        emit TransferAndCall(msg.sender, _to, _value, _methodName, _args);

        // call receiver
        require(Utils.isContract(_to));

        (bool success, bytes memory data) = _to.call(abi.encodePacked(abi.encodeWithSelector(_methodName, msg.sender, _value), _args));
        require(success);
        return data;
    }

    //ERC 865 + delegate transfer and call
    function transferPreSigned(bytes memory _signature, address _to, uint256 _value, uint256 _fee, uint256 _nonce) public returns (bool) {

        require(!signatures[_signature]);
        bytes32 hashedTx = Utils.transferPreSignedHashing(address(this), _to, _value, _fee, _nonce);
        address from = Utils.recover(hashedTx, _signature);

        require(from != address(0));

        doTransfer(from, _to, _value, _fee, msg.sender);
        signatures[_signature] = true;

        emit Transfer(from, _to, _value);
        emit Transfer(from, msg.sender, _fee);
        emit TransferPreSigned(from, _to, msg.sender, _value, _fee);
        return true;
    }

    function transferAndCallPreSigned(bytes memory _signature, address _to, uint256 _value, uint256 _fee, uint256 _nonce,
        bytes4 _methodName, bytes memory _args) public returns (bytes memory) {

        require(!signatures[_signature]);
        bytes32 hashedTx = Utils.transferAndCallPreSignedHashing(address(this), _to, _value, _fee, _nonce, _methodName, _args);
        address from = Utils.recover(hashedTx, _signature);

        require(from != address(0));

        doTransfer(from, _to, _value, _fee, msg.sender);
        signatures[_signature] = true;

        emit Transfer(from, _to, _value);
        emit Transfer(from, msg.sender, _fee);
        emit TransferAndCallPreSigned(from, _to, msg.sender, _value, _fee, _methodName, _args);

        // call receiver
        require(Utils.isContract(_to));

        //call on behalf of from and not msg.sender
        (bool success, bytes memory data) = _to.call(abi.encodePacked(abi.encodeWithSelector(_methodName, from, _value), _args));
        require(success);
        return data;
    }
}
