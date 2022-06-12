package org.jellyfin.androidtv.data.querying;

public class AdditionalPartsQuery {
        private String ItemId;

        public AdditionalPartsQuery(String itemId) {
            ItemId = itemId;
        }

        public String getItemId() {
            return ItemId;
        }
}
