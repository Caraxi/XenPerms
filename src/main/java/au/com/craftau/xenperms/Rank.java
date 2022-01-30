package au.com.craftau.xenperms;

import java.util.ArrayList;
import java.util.HashMap;

public class Rank {

    public static HashMap<String, Rank> parseRanks(ArrayList<String> ranks){
        HashMap<String, Rank> r = new HashMap<>();
        for (int i=0;i<ranks.size();i++) {
            Rank rank = new Rank(i, ranks.get(i));
            r.put(rank.id, rank);
        }
        return r;
    }


    public String id;
    public String name;
    public int index;

    public Rank(int index, String s) {
        this(index, s.split(":", 2));
    }

    public Rank(int index, String[] s) {
        this(index, s[0], s[1]);
    }

    public Rank(int index, String id, String name) {
        this.index = index;
        this.id = id;
        this.name = name;
    }

}
