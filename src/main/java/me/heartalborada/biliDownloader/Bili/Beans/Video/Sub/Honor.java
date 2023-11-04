package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Honor {

    private int aid;
    private int type;
    private String desc;
    @SerializedName("weekly_recommend_num")
    private int weeklyRecommendNum;

}