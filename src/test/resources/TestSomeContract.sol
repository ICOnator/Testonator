pragma solidity 0.5.6;

contract TestSomeContract {

    event FunctionNoArgs(address testFrom, bool testBoolean, uint256 testValue);
    event FunctionSimpleArgs(address testFrom, bool testBoolean, uint256 testValue, uint256 testAnotherValue);
    event FunctionCalled(address testFrom, uint256 testValue, bool testBoolean, string testString, address[] testArray);

    function someName(address _from, uint256 _value) public {
        emit FunctionNoArgs(_from, true, _value);
    }

    function someName(address _from, uint256 _value, uint256 _anotherValue) public {
        emit FunctionSimpleArgs(_from, true, _value, _anotherValue);
    }

    function someName(address _from, uint256 _value, bool _testBoolean, string memory _testString, address[] memory _testArray) public {
        emit FunctionCalled(_from, _value, _testBoolean, _testString, _testArray);
    }
}
