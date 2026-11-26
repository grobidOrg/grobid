(:
  Box of every non-empty Illustration in a pdfalto ALTO file, as "page,x,y,width,height".
  pdfalto numbers pages with Page/@PHYSICAL_IMG_NR (1-based). Name tests use *: so the
  query works with or without the ALTO namespace.
:)

for $i in //*:Illustration[number(@WIDTH) > 0 and number(@HEIGHT) > 0]
return string-join(($i/ancestor::*:Page/@PHYSICAL_IMG_NR, $i/@HPOS, $i/@VPOS, $i/@WIDTH, $i/@HEIGHT), ',')
