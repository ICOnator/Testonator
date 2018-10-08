pragma solidity ^0.4.24;


library Utils {

    //From: https://github.com/OpenZeppelin/openzeppelin-solidity/blob/master/contracts/AddressUtils.sol

    /**
    * Returns whether the target address is a contract
    * @dev This function will return false if invoked during the constructor of a contract,
    *  as the code is not actually created until after the constructor finishes.
    * @param addr address to check
    * @return whether the target address is a contract
    */
    function isContract(address addr) internal view returns (bool) {
        uint256 size;
        // XXX Currently there is no better way to check if there is a contract in an address
        // than to check the size of the code at that address.
        // See https://ethereum.stackexchange.com/a/14016/36603
        // for more details about how this works.
        // TODO Check this again before the Serenity release, because all addresses will be
        // contracts then.
        // solium-disable-next-line security/no-inline-assembly
        assembly { size := extcodesize(addr) }
        return size > 0;
    }

    //From: https://github.com/PROPSProject/props-token-distribution/blob/master/contracts/token/ERC865Token.sol

    /**
     * @notice Hash (keccak256) of the payload used by transferPreSigned
     * @param _token address The address of the token.
     * @param _to address The address which you want to transfer to.
     * @param _value uint256 The amount of tokens to be transferred.
     * @param _fee uint256 The amount of tokens paid to msg.sender, by the owner.
     * @param _nonce uint256 Presigned transaction number.
     */
    function transferPreSignedHashing(address _token, address _to, uint256 _value, uint256 _fee,
        uint256 _nonce, bytes4 _methodName, bytes _args) internal pure returns (bytes32) {
        /* "dbe43fca": transferPreSignedHashing(address,address,address,uint256,uint256,uint256,bytes4,bytes) */
        return keccak256(bytes4(0xdbe43fca), _token, _to, _value, _fee, _nonce, _methodName, _args);
    }

    function transferPreSignedHashing(address _token, address _to, uint256 _value, uint256 _fee,
        uint256 _nonce) internal pure returns (bytes32) {
        /* "48664c16": transferPreSignedHashing(address,address,address,uint256,uint256,uint256) */
        return keccak256(bytes4(0x48664c16), _token, _to, _value, _fee, _nonce);
    }

    /**
    * @notice Recover signer address from a message by using his signature
    * @param hash bytes32 message, the hash is the signed message. What is recovered is the signer address.
    * @param sig bytes signature, the signature is generated using web3.eth.sign()
    */
    function recover(bytes32 hash, bytes sig) internal pure returns (address) {bytes32 r; bytes32 s; uint8 v;

        //Check the signature length
        if (sig.length != 65) {
            return (address(0));
        }

        // Divide the signature in r, s and v variables
        assembly {
            r := mload(add(sig, 32))
            s := mload(add(sig, 64))
            v := byte(0, mload(add(sig, 96)))
        }

        // Version of signature should be 27 or 28, but 0 and 1 are also possible versions
        if (v < 27) {
            v += 27;
        }

        // If the version is correct return the signer address
        if (v != 27 && v != 28) {
            return (address(0));
        } else {
            return ecrecover(hash, v, r, s);
        }
    }
}