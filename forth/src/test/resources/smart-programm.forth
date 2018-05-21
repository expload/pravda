\ program receives `mode` `amount` `address`
\ if `mode` equals 1 it adds `amount` to the `address`
\ if `mode` equals 2 it takes `amount` from the `address`

loadData
dup3 1 == if dup1 sget dup3 + dup2 sput then
dup3 2 == if dup1 sget dup3 - dup2 sput then