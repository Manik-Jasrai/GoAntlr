package main

import "fmt"

func main() {
    a := 4      // Node 1: Initial assignment to `a`
    b := 2      // Node 1: Initial assignment to `b`

    if a > b {  // Control flow splits here
        c := a + b  // Node 2: `c` gets a new value (v2)
        a = c
		  // Node 3: `a` gets redefined (v3)
    } else {
        c := b - a  // Node 2 (alternate path): `c` gets another value
        a = c * 2   // Node 3 (alternate path): `a` is redefined again
    }

    // Merge control flow (Node 3): `b` gets its value based on the path taken
    if a > 10 {
        b = 1      // One possible value for `b`
    } else {
        b = 0      // Another possible value for `b`
    }
	
	for i := 0;i<5;i++ {
		a = b + c
		c = b + d
	}

    fmt.Println(a, b) // Final use of `a` and `b`
}
