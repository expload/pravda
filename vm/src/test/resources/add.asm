preconditions:
  stack:
    uint8(4), #1
  heap:
    #0 = "hello world",
    #2 = x00ECF1
  storage:
    "john" = bigint(100),
    "jane" = bigint(2)

expectations:
  stack:
    uint8(4), #1
  heap:
    #0 = "hello world",
    #2 = x00ECF1
  effects:
    sput 0x10 "i",
    sget int8(-1) "am",
    balance x00ECF1 bigint(100),
    accrue xCCAA42 bigint(0),
    withdraw x00ECF1 bigint(1),
    pcreate x0000 x0000,
    pupdate x0000 x0000

---------------------------------------
push uint8(2)
push uint8(2)
add