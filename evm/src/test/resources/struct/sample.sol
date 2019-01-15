pragma solidity >=0.4.22 <0.6.0;



contract Crowdsale {

     struct Funder {
        address addr;
        uint amount;
    }
    
    mapping (uint => Funder) campaigns;
    
    function balanceOfThatf(uint adr) public payable returns (uint balance)
    {
        campaigns[adr] = Funder({addr: msg.sender, amount: msg.value});
        return 0;
    }
    

    
    
}


