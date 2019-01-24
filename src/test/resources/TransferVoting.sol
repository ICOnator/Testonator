
pragma solidity ^0.5.1;

contract ERC20ish {
    function balanceOf(address owner) public view returns (uint256);
    function transfer(address to, uint256 value) public returns (bool);
}

contract TransferVoting {
    ERC20ish public shareContract;

    uint256 public yay = 0;
    uint256 public nay = 0;

    mapping (address => uint256) internal voted;

    event Voted(address indexed from, uint256 value, bool vote);

    constructor(address _shareContract) public {
        //hardcode this
        shareContract = ERC20ish(_shareContract);
    }

    function vote(address _to, uint256 _value, bool _vote) public returns (uint256, uint256) {
        require(msg.sender == address(shareContract)); //needs to be the DOS contract
        require(voted[_to] == 0);
        uint256 votingPower = shareContract.balanceOf(_to);
        voted[_to] = votingPower;
        if(_vote) {
            yay += votingPower;
        } else {
            nay += votingPower;
        }
        emit Voted(msg.sender, votingPower, _vote);
        return (yay, nay);

    }

    function returnTokens(address _to, uint256 _value) public {
        require(msg.sender == address(shareContract)); //needs to be the DOS contract
        uint256 amount = voted[_to];
        voted[_to] = 0;
        shareContract.transfer(_to, amount);
    }
}
