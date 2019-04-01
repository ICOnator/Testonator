pragma solidity 0.5.7;

contract HashTest {

    function transferAndCallPreSignedHashing(address _token, address _to, uint256 _value, uint256 _fee, uint256 _nonce,
        bytes4 _methodName, bytes memory _args) public pure returns (bytes32) {
        /* "38980f82": transferAndCallPreSignedHashing(address,address,uint256,uint256,uint256,bytes4,bytes) */
        return keccak256(abi.encode(bytes4(0x38980f82), _token, _to, _value, _fee, _nonce, _methodName, _args));
    }

    function transferPreSignedHashing(address _token, address _to, uint256 _value, uint256 _fee, uint256 _nonce)
    public pure returns (bytes32) {
        /* "15420b71": transferPreSignedHashing(address,address,uint256,uint256,uint256) */
        return keccak256(abi.encode(bytes4(0x15420b71), _token, _to, _value, _fee, _nonce));
    }
}
