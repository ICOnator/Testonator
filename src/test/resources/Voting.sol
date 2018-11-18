pragma solidity ^0.4.25;

contract ERC20ish {
    function balanceOf(address owner, uint64 fromBlock) public view returns (uint256);
}

contract Voting {
    ERC20ish public shareContract;

    uint64 untilBlock = 10;

    uint256 yay = 0;
    uint256 nay = 0;

    constructor(address _shareContract) public {
        shareContract = ERC20ish(_shareContract);
    }

    function voting(bool vote) public {
        if(vote) {
            yay += shareContract.balanceOf(msg.sender, untilBlock);
        } else {
            nay += shareContract.balanceOf(msg.sender, untilBlock);
        }
    }
}