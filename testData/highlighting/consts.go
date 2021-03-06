package main
import "fmt"

type T int
type TT int

const (
    A , X, Y = T(iota), 1, T(1)
)
const (
    B = T(iota)
    C
    D TT = 1
)

func (t T) String() string {
    return "t"
}

func (t TT) AnotherString() string {
    return "tt"
}

func main() {
    fmt.Println(A.String())
    fmt.Println(B.String())
    fmt.Println(C.String())
    fmt.Println(D.AnotherString())
}