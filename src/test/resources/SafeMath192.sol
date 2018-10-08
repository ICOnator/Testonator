//using: https://github.com/OpenZeppelin/openzeppelin-solidity/blob/c63b203c1d1800c9a8b5f51f0b444187fdc6c185/contracts/math/SafeMath.sol
pragma solidity ^0.4.24;

/**
 * @title SafeMath
 * @dev Math operations with safety checks that revert on error
 */
library SafeMath {

  /**
  * @dev Multiplies two numbers, reverts on overflow.
  */
  function mul(uint192 a, uint192 b) internal pure returns (uint192) {
    // Gas optimization: this is cheaper than requiring 'a' not being zero, but the
    // benefit is lost if 'b' is also tested.
    // See: https://github.com/OpenZeppelin/openzeppelin-solidity/pull/522
    if (a == 0) {
      return 0;
    }

    uint192 c = a * b;
    require(c / a == b);

    return c;
  }

  /**
  * @dev Integer division of two numbers truncating the quotient, reverts on division by zero.
  */
  function div(uint192 a, uint192 b) internal pure returns (uint192) {
    require(b > 0); // Solidity only automatically asserts when dividing by 0
    uint192 c = a / b;
    // assert(a == b * c + a % b); // There is no case in which this doesn't hold

    return c;
  }

  /**
  * @dev Subtracts two numbers, reverts on overflow (i.e. if subtrahend is greater than minuend).
  */
  function sub(uint192 a, uint192 b) internal pure returns (uint192) {
    require(b <= a);
    uint192 c = a - b;

    return c;
  }

  /**
  * @dev Adds two numbers, reverts on overflow.
  */
  function add(uint192 a, uint192 b) internal pure returns (uint192) {
    uint192 c = a + b;
    require(c >= a);

    return c;
  }

  /**
  * @dev Divides two numbers and returns the remainder (unsigned integer modulo),
  * reverts when dividing by zero.
  */
  function mod(uint192 a, uint192 b) internal pure returns (uint192) {
    require(b != 0);
    return a % b;
  }
}

