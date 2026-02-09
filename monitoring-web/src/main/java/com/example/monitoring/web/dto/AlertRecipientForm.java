package com.example.monitoring.web.dto;

import java.util.ArrayList;
import java.util.List;

public class AlertRecipientForm {

    private Long id;                // (페이지/컨트롤러에서 안 써도 됨. 확장용)
    private String name;
    private Boolean enabled = true;

    // 화면에서 체크박스 등으로 다루기 쉽게 List로 둠 (ex: ["SMS","EMAIL"])
    private List<String> channelList = new ArrayList<>();

    private String phone;
    private String email;
    private String kakao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public List<String> getChannelList() { return channelList; }
    public void setChannelList(List<String> channelList) {
        this.channelList = (channelList == null) ? new ArrayList<>() : channelList;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getKakao() { return kakao; }
    public void setKakao(String kakao) { this.kakao = kakao; }

    /** DB의 channels(CSV: "SMS,EMAIL") → form.channelList 로딩 */
    public void loadFromChannelsCsv(String csv) {
        this.channelList.clear();
        if (csv == null || csv.isBlank()) return;

        for (String s : csv.split(",")) {
            String v = s.trim().toUpperCase();
            if (!v.isBlank() && !this.channelList.contains(v)) {
                this.channelList.add(v);
            }
        }
    }

    /** form.channelList → DB 저장용 CSV("SMS,EMAIL") */
    public String toChannelsCsv() {
        if (channelList == null || channelList.isEmpty()) return "";
        List<String> normalized = new ArrayList<>();
        for (String s : channelList) {
            if (s == null) continue;
            String v = s.trim().toUpperCase();
            if (!v.isBlank() && !normalized.contains(v)) normalized.add(v);
        }
        return String.join(",", normalized);
    }
}