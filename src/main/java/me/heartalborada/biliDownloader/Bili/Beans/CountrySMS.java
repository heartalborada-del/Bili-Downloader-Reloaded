package me.heartalborada.biliDownloader.Bili.Beans;

import lombok.Getter;

public class CountrySMS {
    @Getter
    private final int id;
    @Getter
    private final String cname;
    @Getter
    private final int countryID;

    public CountrySMS(int id, String cname, int countryID) {
        this.id = id;
        this.cname = cname;
        this.countryID = countryID;
    }
}
