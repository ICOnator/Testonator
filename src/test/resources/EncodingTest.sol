pragma solidity 0.5.7;

contract EncodingTest {

    event Debug(bytes encoded);

    function encode(bytes4 methodName, bytes memory args, address from, uint256 value)
        public returns (bytes32) {

        bytes memory encoded1 = abi.encodeWithSelector(methodName, from, value, args);
        emit Debug(encoded1);

        bytes memory encoded2 = abi.encodePacked(abi.encodeWithSelector(methodName, from, value), args);
        emit Debug(encoded2);
    }


}
