type Set = (Int => Boolean)

def single(a: Int): Set = _ == a

def union(s1: Set, s2: Set): Set = x => s1(x) || s2(x)

def intersection(s1: Set, s2: Set): Set = x => s1(x) && s2(x)

def filter(s: Set, p: Int => Boolean): Set = x => s(x) && p(x)
//def filter = intersection _

val positives: Set = _ > 0
