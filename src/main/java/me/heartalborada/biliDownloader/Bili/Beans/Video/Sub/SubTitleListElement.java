package me.heartalborada.biliDownloader.Bili.Beans.Video.Sub;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class SubTitleListElement {

    @SerializedName("id")
    private Long id;
    @SerializedName("lan")
    private String lan;
    @SerializedName("lan_doc")
    private String lanDoc;
    @SerializedName("is_lock")
    private Boolean isLock;
    @SerializedName("subtitle_url")
    private String subtitleUrl;
    @SerializedName("type")
    private Integer type;
    @SerializedName("id_str")
    private String idStr;
    @SerializedName("ai_type")
    private Integer aiType;
    @SerializedName("ai_status")
    private Integer aiStatus;
    @SerializedName("author")
    private Owner author;
}
