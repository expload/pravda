pragma solidity >=0.4.22 <0.6.0;

contract SimpleToken {

    mapping(address => uint256) public balanceOf;

    function balanceOfThatf(address tokenOwner) public view returns (uint256 balance)
    {
        return balanceOf[tokenOwner];
    }

    function transfer(address to,uint256 token) public returns  (bool success)
    {
        balanceOf[msg.sender] = balanceOf[msg.sender] - token;
        balanceOf[to] = balanceOf[to] + token;

        return true;
    }

    function emitFunc(address owner,uint256 token) public returns  (bool success)
    {
        balanceOf[owner] = balanceOf[owner] + token;
        return true;
    }
}

