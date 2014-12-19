/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tv.mediabrowser.mediabrowsertv;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import mediabrowser.model.dto.BaseItemDto;
import tv.mediabrowser.mediabrowsertv.R;

/*
 * Details activity class that loads LeanbackDetailsFragment class
 */
public class DetailsActivity extends Activity {
    public static final String SHARED_ELEMENT_NAME = "hero";

    private BaseItemDto mBaseItem;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_blank);
        mBaseItem = TvApp.getApplication().getSerializer().DeserializeFromString(getIntent().getStringExtra("BaseItemDto"),BaseItemDto.class);

        Fragment fragment;
        switch (mBaseItem.getType()) {
            case "Movie":
                fragment = new MovieDetailsFragment();
                break;
            default:
                fragment = new BaseItemDetailsFragment();
        }

        FragmentManager fragMan = getFragmentManager();
        FragmentTransaction fragTran = fragMan.beginTransaction();
        fragTran.add(R.id.detail_replace, fragment);
        fragTran.commit();

    }

    public BaseItemDto getBaseItem(){
        return mBaseItem;
    }

}
