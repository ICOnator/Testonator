pragma solidity 0.5.6;

contract RecoverTests {

    /**
     * @dev Recover signer address from a message by using their signature
     * @param hash bytes32 message, the hash is the signed message. What is recovered is the signer address.
     * @param signature bytes signature, the signature is generated using web3.eth.sign()
     */
    function recoverMalleable(bytes32 hash, bytes memory signature) public pure returns (address) {
        bytes32 r;
        bytes32 s;
        uint8 v;

        // Check the signature length
        if (signature.length != 65) {
            return (address(0));
        }

        // Divide the signature in r, s and v variables
        // ecrecover takes the signature parameters, and the only way to get them
        // currently is to use assembly.
        // solhint-disable-next-line no-inline-assembly
        assembly {
            r := mload(add(signature, 0x20))
            s := mload(add(signature, 0x40))
            v := byte(0, mload(add(signature, 0x60)))
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

    //From: https://github.com/OpenZeppelin/openzeppelin-solidity/blob/master/contracts/cryptography/ECDSA.sol

    /**
    * @notice Recover signer address from a message by using his signature
    * @param hash bytes32 message, the hash is the signed message. What is recovered is the signer address.
    * @param sig bytes signature, the signature is generated using web3.eth.sign()
    */
    function recover(bytes32 hash, bytes memory sig) public pure returns (address) {
        //r is computed as the X coordinate of a point R, modulo the curve order n.
        bytes32 r;
        //s is (hash+rdA) / random number
        bytes32 s;
        //v is used for public key recovery: https://bitcoin.stackexchange.com/questions/38351/ecdsa-v-r-s-what-is-v
        uint8 v;

        //Check the signature length
        if (sig.length != 65) {
            return address(0);
        }

        // Divide the signature in r, s and v variables
        assembly {
            r := mload(add(sig, 32))
            s := mload(add(sig, 64))
            v := byte(0, mload(add(sig, 96)))
        }

        //EIP-2 still allows signature malleabality, remove this possibility
        if (uint256(s) > uint256(0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0)) {
            return address(0);
        }

        //removed the possibility of 0/1 in the signature, see:
        //https://github.com/OpenZeppelin/openzeppelin-solidity/pull/1622
        //https://github.com/ethereum/EIPs/issues/865

        // If the version is correct return the signer address
        // see
        // https://github.com/ethereum/go-ethereum/blob/master/core/types/transaction_signing.go#L195
        if (v != 27 && v != 28) {
            return address(0);
        } else {
            return ecrecover(hash, v, r, s);
        }
    }
}
