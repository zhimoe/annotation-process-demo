package moe.zhi;

public class Name {

    private final String first;
    private final String last;

    public Name(String f, String l) {
        first = f;
        last = l;
    }

    @Comparator("NameByFirstNameComparator")
    public int compareToByFirstName(Name other) {
        if (this == other)
            return 0;
        int result;
        if ((result = this.first.compareTo(other.first)) != 0)
            return result;
        return this.last.compareTo(other.last);
    }
}
