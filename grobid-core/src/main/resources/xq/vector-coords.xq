(:
  Bounding box of every vector graphic group in a pdfalto page SVG (image-N.svg), one
  "x,y,width,height" string per group, in page coordinates.

  pdfalto writes <svg xmlns="http://www.w3.org/2000/svg"><g><path d="M x,y L x,y C ... Z"/></g></svg>
  with absolute M/L/C/Z commands only, so every number in @d belongs to an x,y pair.
  Curve control points are included, so a curved shape can get a slightly larger box.
  Name tests use *: so the query works with or without the SVG namespace.
:)

declare function local:numbers($d as xs:string) as xs:double* {
  for $t in tokenize(normalize-space(replace($d, '[A-Za-z,]', ' ')), ' ')
  return xs:double($t)
};

for $g in //*:g
let $xs :=
  for $p in $g/*:path
  return local:numbers(string($p/@d))[position() mod 2 = 1]
let $ys :=
  for $p in $g/*:path
  return local:numbers(string($p/@d))[position() mod 2 = 0]
where exists($xs) and exists($ys)
return string-join(
  for $v in (min($xs), min($ys), max($xs) - min($xs), max($ys) - min($ys))
  return string(round-half-to-even($v, 3)),
  ",")
