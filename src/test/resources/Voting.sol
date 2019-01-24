pragma solidity ^0.5.1;

contract ERC20ish {
    function balanceOf(address owner, uint64 fromBlock) public view returns (uint256);
}

contract Voting {
    ERC20ish public shareContract;

    uint64 public untilBlock = 7;

    uint256 public yay = 0;
    uint256 public nay = 0;

    mapping (address => bool) internal voted;

    event Voted(address indexed from, uint256 value, bool vote);

    constructor(address _shareContract) public {
        //hardcode this
        shareContract = ERC20ish(_shareContract);
    }

    function getAddress() public returns (address) {
        return address(shareContract);
    }

    function vote(bool vote) public {
        //require(!voted[msg.sender]); -> disable for testing
        uint256 votingPower = shareContract.balanceOf(msg.sender, untilBlock);
        if(vote) {
            yay += votingPower;
            emit Voted(msg.sender, votingPower, true);
        } else {
            nay += votingPower;
            emit Voted(msg.sender, votingPower, false);
        }
        //voted[msg.sender] = true; -> disable for testing
    }
}
