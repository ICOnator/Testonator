pragma solidity ^0.5.7;


contract ERC20Snapshot {
    string public constant name = "Snapshot coin";
    string public constant symbol = "SSC";
    uint8 public constant decimals = 0;

    struct Checkpoint {
        uint64 fromBlock; // enough for ~10**19 blocks or ~10**12 years
        uint192 amount; // more than enough to fit totalSupply
    }
    mapping (address => Checkpoint[]) internal balances;
    mapping (address => mapping(address => uint256)) internal allowed;

    bool transfersEnabled = true;

    function mint(address second) public {
        Checkpoint memory tmp1;
        tmp1.fromBlock = uint64(block.number);
        tmp1.amount = 10000;
        balances[msg.sender].push(tmp1);

        Checkpoint memory tmp2;
        tmp2.fromBlock = uint64(block.number);
        tmp2.amount = 5000;
        balances[second].push(tmp2);
    }

    function balanceOf(address _owner) public view returns (uint256) {
        // check for history entries
        if (balances[_owner].length == 0) {
            return 0;
        }

        // return last balance entry
        return uint256(balances[_owner][balances[_owner].length - 1].amount);
    }

    function balanceOf(address _owner, uint64 _fromBlock) public view returns (uint256) {
        // check for history entries
        if (balances[_owner].length == 0) {
            return 0;
        }

        // find corresponding balance entry with binary search
        uint256 min = 0;
        uint256 max = balances[_owner].length - 1;

        while (max > min) {
            uint256 mid = (max + min + 1) / 2;

            if (balances[_owner][mid].fromBlock <= _fromBlock) {
                min = mid;
            } else {
                max = mid - 1;
            }
        }

        return uint256(balances[_owner][min].amount);
    }

    function transfer(address _to, uint256 _value) public returns (bool) {
        require(transfersEnabled);
        require(_to != address(0));

        // check if value parameter is within bounds
        uint192 value = uint192(_value);
        require(uint256(value) == _value);

        // while balanceOf returns an uint256 it can be safely cast to
        // uint192, since this type is used for the internal representation (and
        // the total supply is lower)
        uint192 fromBalance = uint192(balanceOf(msg.sender));
        // check ERC20 conditions
        require(value <= fromBalance);

        // subtract tokens from sender
        balances[msg.sender].push(Checkpoint(
                uint64(block.number),
                fromBalance - value   // no over-/underflow possible
            ));

        // add tokens to receiver
        // note: when _from == _to, it is important to call balanceOf only now
        uint192 toBalance = uint192(balanceOf(_to));
        balances[_to].push(Checkpoint(
                uint64(block.number),
                toBalance + value     // no over-/underflow possible, due to total supply
            ));
        return true;
    }
}
