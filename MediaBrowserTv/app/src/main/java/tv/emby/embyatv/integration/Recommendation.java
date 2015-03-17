package tv.emby.embyatv.integration;

public class Recommendation {
    private RecommendationType type;
    private String itemId;
    private Integer recId;
    private long dateAdded;

    public Recommendation(RecommendationType type, String id) {
        itemId = id;
        this.type = type;
        this.dateAdded = System.currentTimeMillis();
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String mItemId) {
        this.itemId = mItemId;
    }

    public Integer getRecId() {
        return recId;
    }

    public void setRecId(Integer mRecId) {
        this.recId = mRecId;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType mType) {
        this.type = mType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Recommendation) {
            Recommendation compare = (Recommendation)o;
            return compare.getType() == this.getType() && compare.getItemId().equals(this.getItemId());
        }
        return false;
    }
}
