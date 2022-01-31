package icbmtherobot;

public class Triple<A, B, C> {
    A valA;
    B valB;
    C valC;

    Triple(A valA, B valB, C valC) {
        this.valA = valA;
        this.valB = valB;
        this.valC = valC;
    }
    public A getA() { return valA; }
    public B getB() { return valB; }
    public C getC() { return valC; }
}
