/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * You can find source here:
 * https://code.google.com/p/dashclock/source/browse/main/src/main/java/com/google/android/apps/dashclock/configuration/ColorPreference.java 
 */

package tv.emby.embyatv.settings;

import tv.emby.embyatv.R;


import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * ColorDialog extracted from {@link ColorPreference}.
 * It can be used as DialogFragment.
 * 
 * 
 * 
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 *
 */
public class ColorPickerDialogDash extends DialogFragment {
	
    private ColorGridAdapter mAdapter;
    private GridView mColorGrid;
    private int[] mColorChoices = {};
    private int mValue = 0;
    private int mItemLayoutId = R.layout.dash_grid_item_color;
    private int mNumColumns = 5;
    
    //---------------------------------------------------------------------------------------------------
    //Added
    //---------------------------------------------------------------------------------------------------
    protected int mSelectedColor; 
    protected int mTitleResId = R.string.lbl_select_color;
    protected OnColorSelectedListener mListener;
    
    //Bundle
    protected static final String KEY_TITLE_ID = "title_id";
    protected static final String KEY_COLORS = "colors";
    protected static final String KEY_SELECTED_COLOR = "selected_color";
    protected static final String KEY_COLUMNS = "columns";
    
    
    
    public ColorPickerDialogDash() {
    }

    public static ColorPickerDialogDash newInstance() {
        return new ColorPickerDialogDash();
    }

    //---------------------------------------------------------------------------------------------------
    // Added 
    //---------------------------------------------------------------------------------------------------
    
    /**
     * Constructor 
     * 
     * @param titleResId       title resource id
     * @param colors           array of colors
     * @param selectedColor    selected color
     * @param columns          number of columns
     * @return                 new ColorPickerDialog
     */
    public static ColorPickerDialogDash newInstance(int titleResId,int[] colors, int selectedColor,int columns) {
    	ColorPickerDialogDash colorPicker = ColorPickerDialogDash.newInstance();
    	colorPicker.initialize(titleResId, colors, selectedColor, columns);
    	return colorPicker;
    }
    
    public void setArguments(int titleResId, int columns) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TITLE_ID, titleResId);
        bundle.putInt(KEY_COLUMNS, columns);
        setArguments(bundle);
    }
    
    /**
     * Interface for a callback when a color square is selected.
     */
    public interface OnColorSelectedListener {

        /**
         * Called when a specific color square has been selected.
         */
        public void onColorSelected(int color);
    }
    
    
    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
            mListener = listener;
    }
    
    /**
     * Initialize the dialog picker
     * 
     * @param titleResId       title resource id
     * @param colors           array of colors
     * @param selectedColor    selected color
     * @param columns          number of columns
     */
    public void initialize(int titleResId, int[] colors, int selectedColor, int columns) {
    	mColorChoices= colors;
    	mNumColumns = columns;
    	mSelectedColor= selectedColor;
    	if (titleResId>0)
    		mTitleResId=titleResId;
    	
    	setArguments(mTitleResId, mNumColumns);
    }
    
    
    //---------------------------------------------------------------------------------------------------
    
    public void setPreference() {
        tryBindLists();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mColorChoices.length>0)
        	tryBindLists();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View rootView = layoutInflater.inflate(R.layout.dash_dialog_colors, null);
        
        if (getArguments() != null) {
            mTitleResId = getArguments().getInt(KEY_TITLE_ID);
            mNumColumns = getArguments().getInt(KEY_COLUMNS);
        }
        
        if (savedInstanceState != null) {
            mColorChoices = savedInstanceState.getIntArray(KEY_COLORS);
            mSelectedColor = (Integer) savedInstanceState.getSerializable(KEY_SELECTED_COLOR);
            tryBindLists();
        }
        
        
        mColorGrid = (GridView) rootView.findViewById(R.id.color_grid);
        
        mColorGrid.setNumColumns(mNumColumns);
        
        mColorGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> listView, View view,
                    int position, long itemId) {
            	    //Added
            		//Toast.makeText(getActivity(), "Selected color="+mAdapter.getItem(position), Toast.LENGTH_LONG).show();
            		if (mListener!=null)
            			mListener.onColorSelected(mAdapter.getItem(position));
            		dismiss();
            }
        });

        tryBindLists();
       
        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setTitle(mTitleResId) //Added
                .create();
    }

    private void tryBindLists() {
        
        if (isAdded() && mAdapter == null) {
            mAdapter = new ColorGridAdapter();
        }

        if (mAdapter != null && mColorGrid != null) {
             mAdapter.setSelectedColor(mSelectedColor);  //USE this to select color
            mColorGrid.setAdapter(mAdapter);
        }
    }

    private class ColorGridAdapter extends BaseAdapter {
        private List<Integer> mChoices = new ArrayList<Integer>();
        private int mSelectedColor;

        private ColorGridAdapter() {
            for (int color : mColorChoices) {
                mChoices.add(color);
            }
        }

        @Override
        public int getCount() {
            return mChoices.size();
        }

        @Override
        public Integer getItem(int position) {
            return mChoices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mChoices.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(mItemLayoutId, container, false);
            }

            int color = getItem(position);
            setColorViewValue(convertView.findViewById(R.id.color_view), color);
            convertView.setBackgroundColor(color == mSelectedColor
                    ? 0x6633b5e5 : 0);

            return convertView;
        }

        public void setSelectedColor(int selectedColor) {
        	
        	if (mSelectedColor != selectedColor) {
        		mSelectedColor = selectedColor;
        		notifyDataSetChanged();
        	}
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(KEY_COLORS, mColorChoices);
        outState.putSerializable(KEY_SELECTED_COLOR, mSelectedColor);
    }


	private static void setColorViewValue(View view, int color) {
	    if (view instanceof ImageView) {
	        ImageView imageView = (ImageView) view;
	        Resources res = imageView.getContext().getResources();
	
	        Drawable currentDrawable = imageView.getDrawable();
	        GradientDrawable colorChoiceDrawable;
	        if (currentDrawable != null && currentDrawable instanceof GradientDrawable) {
	            // Reuse drawable
	            colorChoiceDrawable = (GradientDrawable) currentDrawable;
	        } else {
	            colorChoiceDrawable = new GradientDrawable();
	            colorChoiceDrawable.setShape(GradientDrawable.OVAL);
	        }
	
	        // Set stroke to dark version of color
	        int darkenedColor = Color.rgb(
	                Color.red(color) * 192 / 256,
	                Color.green(color) * 192 / 256,
	                Color.blue(color) * 192 / 256);
	
	        colorChoiceDrawable.setColor(color);
	        colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(
	                TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics()), darkenedColor);
	        imageView.setImageDrawable(colorChoiceDrawable);
	
	    } else if (view instanceof TextView) {
	        ((TextView) view).setTextColor(color);
	    }
	}
}
