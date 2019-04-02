pragma solidity 0.5.7;

contract ERC20ish {
    function transfer(address to, uint256 value) public returns (bool);
}

contract Inventory {
    ERC20ish public shareContract;

    uint256 public amount = 0;
    address owner;

    struct Item {
        string serialNumber;
        string description;
    }
    mapping (address => Item[]) internal inventory;

    event Itemized(string serialNumber, string description);

    constructor(address _shareContract) public {
        shareContract = ERC20ish(_shareContract);
        owner = msg.sender;
    }

    //itemize(address,uint256,string,string) -> 5c28b451
    function itemize(address _from, uint256 _value, string memory _serialNumber, string memory _description) public {
        require(msg.sender == address(shareContract)); //needs to be the DOS contract
        require(_value == 1); //don't care about safe math
        amount = amount + 1;
        inventory[_from].push(Item(_serialNumber, _description));
        emit Itemized(_serialNumber, _description);
    }

    function itemLength(address _addr) public view returns (uint256) {
        return inventory[_addr].length;
    }

    function itemSerialAt(address _addr, uint256 _index) public view returns (string memory) {
        return inventory[_addr][_index].serialNumber;
    }

    function itemDescriptionAt(address _addr, uint256 _index) public view returns (string memory) {
        return inventory[_addr][_index].description;
    }

    function payout(address _to) public {
        require(msg.sender == owner);
        shareContract.transfer(_to, amount);
    }
}
