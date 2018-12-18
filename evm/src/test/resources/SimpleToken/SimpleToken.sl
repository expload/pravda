pragma solidity >=0.4.22 <0.6.0;

contract SimpleToken {

    mapping(address => uint256) public balances;

    function balanceOf(address tokenOwner) public view returns (uint256 balance)
    {
        return balances[tokenOwner];
    }

    function transfer(address to, uint256 token) public returns  (bool success)
    {
        balances[msg.sender] = balances[msg.sender] - token;
        balances[to] = balances[to] + token;

        return true;
    }

    function emitTokens(address owner, uint256 token) public returns  (bool success)
    {
        balances[owner] = balances[owner] + token;
        return true;
    }
}

