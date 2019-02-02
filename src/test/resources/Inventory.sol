pragma solidity ^0.5.2;

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

    function itemize(address _from, uint256 _value, string memory _serialNumber, string memory _description) public {
        require(msg.sender == address(shareContract)); //needs to be the DOS contract
        require(_value == 1); //don't care about safe math
        amount = amount + 1;
        inventory[_from].push(Item(_serialNumber, _description));
        emit Itemized(_serialNumber, _description);
    }

    function payout(address _to) public {
        require(msg.sender == address(shareContract)); //needs to be the DOS contract
        require(msg.sender == owner);
        shareContract.transfer(_to, amount);
    }

    function transferPreSignedHashing(address _token, address _to, uint256 _value, uint256 _fee) public pure returns (bytes32) {
        /* "5c4b4c12": transferPreSignedHashing(address,address,uint256,uint256) */
        return keccak256(abi.encodePacked(bytes4(0x5c4b4c12), _token, _to, _value, _fee));
    }

    function transferPreSigned(address _token, address _to, uint256 _value, uint256 _fee) public pure returns (bytes memory) {
        /* "5c4b4c12": transferPreSignedHashing(address,address,uint256,uint256) */
        return abi.encode(bytes4(0x5c4b4c12), _token, _to, _value, _fee);
    }
}
