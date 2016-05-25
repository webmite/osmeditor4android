package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;


	
public class TagFormFragment extends BaseFragment implements FormUpdate {

	private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();
	
	/**
	 * Max number of entries before we use a normal dropdown list for selection
	 */
	private static final int MAX_ENTRIES_MULTISELECT = 8;
	
	LayoutInflater inflater = null;

	private Names names = null;

	private Preferences prefs = null;

	private EditorUpdate tagListener = null;

	private NameAdapters nameAdapters = null;
	
	private boolean focusOnAddress = false;
	
	private int maxInlineValues = 3;

	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress) {
    	TagFormFragment f = new TagFormFragment();
    	
        Bundle args = new Bundle();
   
        args.putSerializable("displayMRUpresets", Boolean.valueOf(displayMRUpresets));
        args.putSerializable("focusOnAddress", Boolean.valueOf(focusOnAddress));

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
        	tagListener = (EditorUpdate) context;
            nameAdapters = (NameAdapters) context;
        } catch (ClassCastException e) {
        	throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener and NameAdapters");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       	Log.d(DEBUG_TAG, "onCreate");
    }
	
	/** 
	 * display member elements of the relation if any
	 * @param members 
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	ScrollView rowLayout = null;

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from original arguments");
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
		}
    	
    	
     	this.inflater = inflater;
     	rowLayout = (ScrollView) inflater.inflate(R.layout.tag_form_view, container, false);
           	
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	focusOnAddress = ((Boolean) getArguments().getSerializable("focusOnAddress")).booleanValue();
     	
       	// Log.d(DEBUG_TAG,"element " + element + " tags " + tags);
		
	
		if (getUserVisibleHint()) { // don't request focus if we are not visible 
			Log.d(DEBUG_TAG,"is visible");
		}	
		// 
    	prefs = new Preferences(getActivity());
		
		if (prefs.getEnableNameSuggestions()) {
			names = Application.getNames(getActivity());
		}
		
		maxInlineValues = prefs.getMaxInlineValues();

		if (displayMRUpresets) {
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getChildFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			
			recentPresetsFragment = RecentPresetsFragment.newInstance(((PropertyEditor)getActivity()).getElement()); // FIXME
			ft.add(R.id.form_mru_layout,recentPresetsFragment,"recentpresets_fragment");
			ft.commit();
		}
		
		Log.d(DEBUG_TAG,"onCreateView returning");
		return rowLayout;
	}
    
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.d(DEBUG_TAG, "onStart");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(DEBUG_TAG, "onResume");
    }

	@Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    }  
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    }
       
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	Log.d(DEBUG_TAG, "onDestroyView");
    }


	
	/**
	 * Simpilified version for non-multi-select and preset only situation
	 * @param key
	 * @param value
	 * @param allTags
	 * @return
	 */
	protected ArrayAdapter<?> getValueAutocompleteAdapter(String key, ArrayList<String> values, PresetItem preset, LinkedHashMap<String, String> allTags) {
		ArrayAdapter<?> adapter = null;
	
		if (key != null && key.length() > 0) {
			Set<String> usedKeys = allTags.keySet();
			if (TagEditorFragment.isStreetName(key, usedKeys)) {
				adapter = nameAdapters.getStreetNameAdapter(values);
			} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
				adapter = nameAdapters.getPlaceNameAdapter(values);
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
				Log.d(DEBUG_TAG,"generate suggestions for name from name suggestion index");
				ArrayList<NameAndTags> suggestions = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(allTags)); 
				if (suggestions != null && !suggestions.isEmpty()) {
					ArrayList<NameAndTags> result = suggestions;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				HashMap<String, Integer> counter = new HashMap<String, Integer>();
				ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<ValueWithCount>(getActivity(), R.layout.autocomplete_row);
	
				Collection<StringWithDescription> presetValues = preset.getAutocompleteValues(key);
				Log.d(DEBUG_TAG,"setting autocomplete adapter for values " + presetValues);
				if (values != null && !values.isEmpty()) {
					ArrayList<StringWithDescription> result = new ArrayList<StringWithDescription>(presetValues);
					if (preset.sortIt(key)) {
						Collections.sort(result);
					}
					for (StringWithDescription s:result) {
						if (counter != null && counter.containsKey(s.getValue())) {
							continue; // skip stuff that is already listed
						}
						counter.put(s.getValue(),Integer.valueOf(1));
						adapter2.add(new ValueWithCount(s.getValue(), s.getDescription(), true));
					}
					Log.d(DEBUG_TAG,"key " + key + " type " + preset.getKeyType(key));
				} 
				if (!counter.containsKey("") && !counter.containsKey(null)) { // add empty value so that we can remove tag
					adapter2.insert(new ValueWithCount("", getString(R.string.tag_not_set), true),0); // FIXME allow unset value depending on preset
				}
				if (values != null) { // add in any non-standard non-empty values
					for (String value:values) {
						if (!"".equals(value) && !counter.containsKey(value)) {
							ValueWithCount v = new ValueWithCount(value,1); // FIXME determine description in some way
							adapter2.insert(v,0);
						}
					}
				}	
				Log.d(DEBUG_TAG,adapter2==null ? "adapter2 is null": "adapter2 has " + adapter2.getCount() + " elements");
				if (adapter2.getCount() > 0) {
					return adapter2;
				}

			}
		}
		Log.d(DEBUG_TAG,adapter==null ? "adapter is null": "adapter has " + adapter.getCount() + " elements");
		return adapter;
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.tag_form_menu, menu);
		menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(NetworkStatus.isConnected(getActivity()));
	}
	
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Log.d(DEBUG_TAG,"home pressed");
			updateEditorFromText();
			((PropertyEditor)getActivity()).sendResultAndFinish();
			return true;
		case R.id.tag_menu_address:
			updateEditorFromText();
			tagListener.predictAddressTags(true);
			update();
			if (!focusOnValue(Tags.KEY_ADDR_HOUSENUMBER)) {
				focusOnValue(Tags.KEY_ADDR_STREET);
			} 
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = tagListener.getBestPreset();
			if (pi!=null) {
				((PropertyEditor)getActivity()).onPresetSelected(pi, true);
			}
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			startActivity(Preset.getMapFeaturesIntent(getActivity(),tagListener.getBestPreset()));
			return true;
//		case R.id.tag_menu_delete_unassociated_tags:
//			// remove tags that don't belong to an identified preset
//			return true;
		case R.id.tag_menu_resetMRU:
			for (Preset p:((PropertyEditor)getActivity()).presets)
				p.resetRecentlyUsed();
			((PropertyEditor)getActivity()).recreateRecentPresetView();
			return true;
		case R.id.tag_menu_reset_address_prediction:
			// simply overwrite with an empty file
			Address.resetLastAddresses(getActivity());
			return true;
		case R.id.tag_menu_help:
			HelpViewer.start(getActivity(), R.string.help_propertyeditor);
			return true;
		}		
		return false;
	}

	/**
	 * reload original arguments
	 */
	private void doRevert() {
		tagListener.revertTags();
		update();
	}
	
	/**
	 * update editor with any potential text changes that haven't been saved yet
	 */
	private boolean updateEditorFromText() {
		Log.d(DEBUG_TAG,"updating data from last text field");
		// check for focus on text field
		View fragementView = getView();
		if (fragementView == null) {
			return false; // already destroyed?
		}
		LinearLayout l = (LinearLayout) fragementView.findViewById(R.id.form_container_layout);
		if (l != null) { // FIXME this might need an alert
			View v = l.findFocus();
			Log.d(DEBUG_TAG,"focus is on " + v);
			if (v != null && v instanceof CustomAutoCompleteTextView){
				View row = v;
				do {
					row = (View) row.getParent();
				} while (row != null && !(row instanceof TagTextRow));
				if (row != null) {
					tagListener.updateSingleValue(((TagTextRow) row).getKey(), ((TagTextRow) row).getValue());
				}
			}
		}
		return true;
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getImmutableView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.form_immutable_row_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.form_immutable_row_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.form_immutable_row_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.form_immutable_row_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
	

	public void enableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).enable();
		}
	}
	
	public void disableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).disable();
		}
	}
	
	protected void recreateRecentPresetView() {
		Log.d(DEBUG_TAG,"Updating MRU prests");
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
		}
	}
	
	public void update() {
		Log.d(DEBUG_TAG,"update");
		// remove all editable stuff
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			while (ll.getChildAt(0) instanceof EditableLayout) {
				ll.removeViewAt(0);
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return;
		}		
		final EditableLayout editableView  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, ll, false);
		editableView.setSaveEnabled(false); 
		int pos = 0;
		ll.addView(editableView, pos++);
		
		LinearLayout nonEditableView = (LinearLayout) getImmutableView();
		if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
			nonEditableView.removeAllViews(); 
		}
		
    	PresetItem mainPreset = tagListener.getBestPreset();
    	editableView.setTitle(mainPreset);
    	
    	LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
    	Map<String, String> nonEditable;
    	if (mainPreset != null) {
    		nonEditable = addTagsToViews(editableView, mainPreset, allTags);
    		for (PresetItem preset:tagListener.getSecondaryPresets()) {
    			final EditableLayout editableView1  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, ll, false);
    			editableView1.setSaveEnabled(false);
    			editableView1.setTitle(preset);
    			ll.addView(editableView1, pos++);
    			nonEditable = addTagsToViews(editableView1, preset, (LinkedHashMap<String, String>) nonEditable);
    		}
    	} else {
    		nonEditable = allTags;
    	}
    	
    	LinearLayout nel = (LinearLayout) getView().findViewById(R.id.form_immutable_header_layout);
    	if (nel != null) {
    		nel.setVisibility(View.GONE);
    	}
    	if (nonEditable.size() > 0) {
    		nel.setVisibility(View.VISIBLE);
    		for (String key:nonEditable.keySet()) {
    			addRow(nonEditableView,key, nonEditable.get(key),null, null);
    		}
    	}
    	
    	if (focusOnAddress) {
    		focusOnAddress = false; // only do it once
    		if (!focusOnValue(Tags.KEY_ADDR_HOUSENUMBER)) {
    			if (!focusOnValue(Tags.KEY_ADDR_STREET)) {
    				focusOnEmpty();
    			}
    		} 
    	} else {
    		focusOnEmpty(); 
    	}
	}
	
	Map<String,String> addTagsToViews(LinearLayout editableView, PresetItem preset, LinkedHashMap<String, String> tags) {
		LinkedHashMap<String,String> recommendedEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> optionalEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> linkedTags = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> nonEditable = new LinkedHashMap<String,String>();
		HashMap<String,PresetItem> keyToLinkedPreset = new HashMap<String,PresetItem>();
		
		if (preset != null) {
			List<PresetItem> linkedPresets = preset.getLinkedPresets();
			for (String key:tags.keySet()) {
				if (preset.hasKeyValue(key, tags.get(key))) {
					if (preset.isFixedTag(key)) {
						// skip
					} else if (preset.isRecommendedTag(key)) {
						recommendedEditable.put(key, tags.get(key));
					} else {
						optionalEditable.put(key, tags.get(key));
					}
				} else {
					boolean found = false;
					if (linkedPresets != null) { // check if tag is in a linked preset
						for (PresetItem l:linkedPresets) {
							if (l.hasKeyValue(key, tags.get(key))) {
								linkedTags.put(key, tags.get(key));
								keyToLinkedPreset.put(key, l);
								found = true;
								break;
							}
						}
					}
					if (!found) {
						nonEditable.put(key, tags.get(key));
					}
				}
			}
		} else {
			Log.e(DEBUG_TAG,"addTagsToViews called with null preset");
		}
		for (String key:recommendedEditable.keySet()) {
			addRow(editableView,key, recommendedEditable.get(key),preset, tags);
		}
		for (String key:optionalEditable.keySet()) {
			addRow(editableView,key, optionalEditable.get(key),preset, tags);
		}
		for (String key: linkedTags.keySet()) {
			addRow(editableView,key, linkedTags.get(key), keyToLinkedPreset.get(key), tags);
		}

		return nonEditable;
	}
	
	private void addRow(LinearLayout rowLayout, final String key, final String value, PresetItem preset, LinkedHashMap<String, String> allTags) {
		if (rowLayout != null) {
			if (preset != null) {
				if (!preset.isFixedTag(key)) {
					ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, Util.getArrayList(value), preset, allTags);
					int count = 0;
					if (adapter!=null) {
						count = adapter.getCount();
					} else {
						Log.d(DEBUG_TAG,"adapter null " + key + " " + value + " " + preset);
					}
					String hint = preset.getHint(key);
					//
					PresetKeyType keyType = preset.getKeyType(key);
					String defaultValue = preset.getDefault(key);
					
					if (keyType == PresetKeyType.TEXT 
						|| key.startsWith(Tags.KEY_ADDR_BASE)
						|| (keyType == PresetKeyType.COMBO && preset.isEditable(key))
						|| (keyType == PresetKeyType.MULTISELECT && count > MAX_ENTRIES_MULTISELECT)) {
						rowLayout.addView(addTextRow(rowLayout, preset, keyType, hint, key, value, defaultValue, adapter));
					} else if (preset.getKeyType(key) == PresetKeyType.COMBO || (keyType == PresetKeyType.CHECK && count > 2)) {
						if (count <= maxInlineValues) {
							rowLayout.addView(addComboRow(rowLayout, preset, hint, key, value, defaultValue, adapter));
						} else {
							rowLayout.addView(addComboDialogRow(rowLayout, preset, hint, key, value, defaultValue, adapter));
						}
					} else if (preset.getKeyType(key) == PresetKeyType.CHECK) {
						final TagCheckRow row = (TagCheckRow)inflater.inflate(R.layout.tag_form_check_row, rowLayout, false);
						row.keyView.setText(hint != null?hint:key);
						row.keyView.setTag(key);
						
						String v = "";
						String description = "";
						final String valueOn = preset.getOnValue(key);
						String tempValueOff = "";
						
						// this is a bit of a roundabout way of determining the non-checked value;
						for (int i=0;i< adapter.getCount();i++) {
							Object o = adapter.getItem(i);
							StringWithDescription swd = new StringWithDescription(o);
							v = swd.getValue();
							description = swd.getDescription();
							if (!v.equals(valueOn)) {
								tempValueOff = v;
							}
						}
						
						final String valueOff = tempValueOff;
						
						Log.d(DEBUG_TAG,"adapter size " + adapter.getCount() + " checked value >" + valueOn + "< not checked value >" + valueOff + "<");
						if (description==null) {
							description=v;
						}
						
						row.getCheckBox().setChecked(valueOn.equals(value));
						
						rowLayout.addView(row);
						row.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								tagListener.updateSingleValue(key, isChecked?valueOn:valueOff);
							} 
						});
					} else if (preset.getKeyType(key) == PresetKeyType.MULTISELECT) {
						ArrayList<String> list = new ArrayList<String>();
						char delimiter = preset.getDelimiter(key);
						String delimterString = String.valueOf(delimiter);
						if (value.contains(delimterString)) {
							// while this would seem to be wasteful there is no simple way to avoid recreating the adapter
							// NOTE if there is more than one custom value MAX_ENTRIES_MULTISELECT can be exceeded
							String[] values = value.split(delimterString);
							for (String s:values) {
								list.add(s.trim());
							}
							adapter = getValueAutocompleteAdapter(key, list, preset, allTags);
							count = adapter.getCount();
						} else {
							list.add(value);
						}
						final TagMultiselectRow row = (TagMultiselectRow)inflater.inflate(R.layout.tag_form_multiselect_row, rowLayout, false);
						row.keyView.setText(hint != null?hint:key);
						row.keyView.setTag(key);
						row.setDelimiter(preset.getDelimiter(key));
						CompoundButton.OnCheckedChangeListener  onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								tagListener.updateSingleValue(key, row.getValue());
							} 
						};
						for (int i=0;i< count;i++) {
							Object o = adapter.getItem(i);
							StringWithDescription swd = new StringWithDescription(o);
							String v = swd.getValue();
							String description = swd.getDescription();
							if (v==null || "".equals(v)) {
								continue;
							}
							if (description==null) {
								description=v;
							}

							if ("".equals(value) && (defaultValue != null && !"".equals(defaultValue))) {
								row.addCheck(description, v, v.equals(defaultValue), onCheckedChangeListener);
							} else {
								row.addCheck(description, v, list.contains(v), onCheckedChangeListener);
							}
						}
						
						rowLayout.addView(row);
					}
				}
//			} else if (key.startsWith(Tags.KEY_ADDR_BASE)) { // make address tags always editable
//				Set<String> usedKeys = allTags.keySet();
//				ArrayAdapter<?> adapter = null;
//				if (TagEditorFragment.isStreetName(key, usedKeys)) {
//					adapter = nameAdapters.getStreetNameAutocompleteAdapter(Util.getArrayList(value));
//				} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
//					adapter = nameAdapters.getPlaceNameAutocompleteAdapter(Util.getArrayList(value));
//				}
//				// String hint = preset.getHint(key);
//				rowLayout.addView(addTextRow(null, null, key, value, adapter));
			} else {
				final TagStaticTextRow row = (TagStaticTextRow)inflater.inflate(R.layout.tag_form_static_text_row, rowLayout, false);
				row.keyView.setText(key);
				row.valueView.setText(value);
				rowLayout.addView(row);
			}
		} else {
 			Log.d(DEBUG_TAG, "addRow rowLayout null");
 		}	
	}
	
	TagTextRow addTextRow(LinearLayout rowLayout, PresetItem preset, PresetKeyType keyType, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagTextRow row = (TagTextRow)inflater.inflate(R.layout.tag_form_text_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.valueView.setEllipsize(TruncateAt.END);
		}
		if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
			row.valueView.setText(defaultValue);
		} else {
			row.valueView.setText(value);
		}
		if (adapter != null) {
			row.valueView.setAdapter(adapter);
		} else {
			Log.e(DEBUG_TAG,"adapter null");
			row.valueView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, new String[0]));
		}
		if (keyType==PresetKeyType.MULTISELECT) { 
			// FIXME this should be somewhere better obvious since it creates a non obvious side effect
			row.valueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
		}
		if (keyType==PresetKeyType.TEXT && (adapter==null || adapter.getCount() < 2)) {
			row.valueView.setHint(R.string.tag_value_hint);
		} else {
			row.valueView.setHint(R.string.tag_autocomplete_value_hint);
		}
		OnClickListener autocompleteOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.hasFocus()) {
					Log.d(DEBUG_TAG,"onClick");
					((CustomAutoCompleteTextView)v).showDropDown();
				}
			}
		};
		row.valueView.setOnClickListener(autocompleteOnClick);
		row.valueView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && !row.getValue().equals(value)) {
					Log.d(DEBUG_TAG,"onFocusChange");
					tagListener.updateSingleValue(key, row.getValue());
				}
			}
		});
		row.valueView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d(DEBUG_TAG,"onItemClicked value");
				Object o = parent.getItemAtPosition(position);
				if (o instanceof Names.NameAndTags) {
					row.valueView.setOrReplaceText(((NameAndTags)o).getName());
					// applyTagSuggestions(((NameAndTags)o).getTags());
				} else if (o instanceof ValueWithCount) {
					row.valueView.setOrReplaceText(((ValueWithCount)o).getValue());
				} else if (o instanceof StringWithDescription) {
					row.valueView.setOrReplaceText(((StringWithDescription)o).getValue());
				} else if (o instanceof String) {
					row.valueView.setOrReplaceText((String)o);
				}
				tagListener.updateSingleValue(key, row.getValue());
			}
		});
		
		return row;
	}
	
	TagComboRow addComboRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagComboRow row = (TagComboRow)inflater.inflate(R.layout.tag_form_combo_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
				row.addButton(description, v, v.equals(defaultValue));
			} else {
				row.addButton(description, v, v.equals(value));
			}
		}
		
		row.getRadioGroup().setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Log.d(DEBUG_TAG,"radio group onCheckedChanged");
				String value = "";
				if (checkedId != -1) {
					RadioButton button = (RadioButton) group.findViewById(checkedId);
					value = (String)button.getTag();	
				} 
				tagListener.updateSingleValue(key, value);
				row.setValue(value);
				row.setChanged(true);
			}
		});
		return row;
	}
	
	TagComboDialogRow addComboDialogRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagComboDialogRow row = (TagComboDialogRow)inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue)) && v.equals(defaultValue)) {
				row.setValue(description,v);
				break;
			} else if (v.equals(value)){
				row.setValue(swd);
				break;
			}
		}
		row.valueView.setHint(R.string.tag_dialog_value_hint);
		row.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(DEBUG_TAG,"button clicked ");
				buildComboDialog(hint != null?hint:key,key,defaultValue,adapter,row).show();
			}
		});
		return row;
	}

	
	protected AlertDialog buildComboDialog(String hint,String key,String defaultValue,final ArrayAdapter<?> adapter,final TagComboDialogRow row) {
		String value = row.getValue();
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(hint);
	   	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	
		final View layout = inflater.inflate(R.layout.form_combo_dialog, null);
		RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
		builder.setView(layout);
		
		View.OnClickListener listener = new View.OnClickListener(){
			@Override
			public void onClick(View v) {
			 Log.d(DEBUG_TAG,"radio button clicked " + row.getValue() + " " + v.getTag());
				if (!row.hasChanged()) {
					RadioGroup g = (RadioGroup) v.getParent();
					g.clearCheck();
				} else {
					row.setChanged(false);
				}
			}
		};
		
		LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
		buttonLayoutParams.width = LayoutParams.FILL_PARENT;
		
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			
			if (v==null || "".equals(v)) {
				continue;
			}
			
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
				addButton(getActivity(), valueGroup, i, swd, v.equals(defaultValue), listener, buttonLayoutParams);
			} else {			
				addButton(getActivity(), valueGroup, i, swd, v.equals(value), listener, buttonLayoutParams);
			}
		}
		final Handler handler = new Handler(); 
		builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				tagListener.updateSingleValue((String) layout.getTag(), "");
				row.setValue("","");
				row.setChanged(true);
				final DialogInterface finalDialog = dialog;
				// allow a tiny bit of time to see that the action actually worked
				handler.postDelayed(new Runnable(){@Override public void run() {finalDialog.dismiss();}}, 100);	
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		final AlertDialog dialog = builder.create();
		layout.setTag(key);
		valueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Log.d(DEBUG_TAG,"radio group onCheckedChanged");
				StringWithDescription value = null;
				if (checkedId != -1) {
					RadioButton button = (RadioButton) group.findViewById(checkedId);
					value = (StringWithDescription)button.getTag();	
					tagListener.updateSingleValue((String) layout.getTag(), value.getValue());
					row.setValue(value);
					row.setChanged(true);
				}
				// allow a tiny bit of time to see that the action actually worked
				handler.postDelayed(new Runnable(){@Override public void run() {dialog.dismiss();}}, 100);
			}
		});
		return dialog;
	}

	/**
	 * Focus on the value field of a tag with key "key" 
	 * @param key
	 * @return
	 */
	private boolean focusOnValue( String key) {
		boolean found = false;
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			int pos = 0;
			while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
				EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
				for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
					View v = ll2.getChildAt(i);
					if (v instanceof TagTextRow && ((TagTextRow)v).getKey().equals(key)) {
						((TagTextRow)v).getValueView().requestFocus();
						Util.scrollToRow(sv, v, true, true);
						found = true;
						break;
					}
				}
				pos++;
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return false;
		}	
		return found;
	}
	
	/**
	 * Focus on the first empty value field 
	 * @return
	 */
	private boolean focusOnEmpty() {
		boolean found = false;
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			int pos = 0;
			while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
				EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
				for (int i = 0 ; i < ll2.getChildCount(); i++) {
					View v = ll2.getChildAt(i);
					if (v instanceof TagTextRow && "".equals(((TagTextRow)v).getValue())) {
						((TagTextRow)v).getValueView().requestFocus();
						found = true;
						break;
					}
				}
				pos++;
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return false;
		}	
		return found;
	}
	
	public static class TagTextRow extends LinearLayout {

		private TextView keyView;
		private CustomAutoCompleteTextView valueView;
		
		public TagTextRow(Context context) {
			super(context);
		}
		
		public TagTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (CustomAutoCompleteTextView)findViewById(R.id.textValue);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public String getValue() { 
			return valueView.getText().toString();
		}
		
		public CustomAutoCompleteTextView getValueView() {
			return valueView;
		}
	}
	
	public static class TagStaticTextRow extends LinearLayout {

		private TextView keyView;
		private TextView valueView;
		
		public TagStaticTextRow(Context context) {
			super(context);
		}
		
		public TagStaticTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (TextView)findViewById(R.id.textValue);
		}	
	}
	
	public static class TagComboRow extends LinearLayout {

		private TextView keyView;
		private RadioGroup valueGroup;
		private String value;
		private Context context;
		private int idCounter = 0;
		private boolean changed = false;
		
		public TagComboRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagComboRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueGroup = (RadioGroup)findViewById(R.id.valueGroup);
			
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public RadioGroup getRadioGroup() { 
			return valueGroup;
		}
		
		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
		
		public void setChanged(boolean changed) {
			this.changed = changed;
		}
		
		public boolean hasChanged() {
			return changed;
		}
		
		public void addButton(String description, String value, boolean selected) {
			final AppCompatRadioButton button = new AppCompatRadioButton(context);
			button.setText(description);
			button.setTag(value);
			button.setChecked(selected);
			button.setId(idCounter++);
			valueGroup.addView(button);
			if (selected) {
				setValue(value);
			}
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d(DEBUG_TAG,"radio button clicked " + getValue() + " " + button.getTag());
					if (!changed) {
						RadioGroup g = (RadioGroup) v.getParent();
						g.clearCheck();
					} else {
						changed = false;
					}
				}
			});
		}
	}
	
	public void addButton(Context context, RadioGroup group, int id, StringWithDescription swd, boolean selected, View.OnClickListener listener, LayoutParams layoutParams) {
		final AppCompatRadioButton button = new AppCompatRadioButton(context);
		String description = swd.getDescription();
		button.setText(description != null && !"".equals(description)?description:swd.getValue());
		button.setTag(swd);
		button.setChecked(selected);
		button.setId(id);
		button.setLayoutParams(layoutParams);
		group.addView(button);
		if (selected) {
			// setValue(value);
		}
		button.setOnClickListener(listener);
	}
	
	public static class TagComboDialogRow extends LinearLayout {

		private TextView keyView;
		private TextView valueView;
		private String value;
		private Context context;
		private boolean changed = false;
		
		public TagComboDialogRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagComboDialogRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (TextView)findViewById(R.id.textValue);	
		}
		
		public void setOnClickListener(OnClickListener listener) {
			valueView.setOnClickListener(listener);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public void setValue(String value, String description) {
			this.value = value;
			valueView.setText(description);
		}
		
		public void setValue(StringWithDescription swd) {
			String description = swd.getDescription();
			setValue(swd.getValue(),description != null && !"".equals(description)?description:swd.getValue());
		}

		public String getValue() {
			return value;
		}
		
		public void setChanged(boolean changed) {
			this.changed = changed;
		}
		
		public boolean hasChanged() {
			return changed;
		}
	}
	
	public static class TagMultiselectRow extends LinearLayout {

		private TextView keyView;
		private LinearLayout valueLayout;
		private Context context;
		private char delimiter;
		
		public TagMultiselectRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagMultiselectRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueLayout = (LinearLayout)findViewById(R.id.valueGroup);
			
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public LinearLayout getValueGroup() { 
			return valueLayout;
		}
		
		/**
		 * Return all checked values concatenated with the required delimiter
		 * @return
		 */
		public String getValue() {
			StringBuilder result = new StringBuilder();
			for (int i=0;i<valueLayout.getChildCount();i++) {
				AppCompatCheckBox check = (AppCompatCheckBox) valueLayout.getChildAt(i);
				if (check.isChecked()) {
					if (result.length() > 0) { // not the first entry
						result.append(delimiter);
					}
					result.append(valueLayout.getChildAt(i).getTag());
				}
			}
			return result.toString();
		}
		
		public void setDelimiter(char delimiter) {
			this.delimiter = delimiter;
		}
		
		public void addCheck(String description, String value, boolean selected, CompoundButton.OnCheckedChangeListener listener) {
			final AppCompatCheckBox check = new AppCompatCheckBox(context);
			check.setText(description);
			check.setTag(value);
			check.setChecked(selected);
			valueLayout.addView(check);
			check.setOnCheckedChangeListener(listener);
		}
	}
	
	public static class TagCheckRow extends LinearLayout {

		private TextView keyView;
		private AppCompatCheckBox valueCheck;
		
		public TagCheckRow(Context context) {
			super(context);
		}
		
		public TagCheckRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueCheck = (AppCompatCheckBox)findViewById(R.id.valueSelected);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public AppCompatCheckBox getCheckBox() {
			return valueCheck;
		}
		
		public boolean isChecked() { 
			return valueCheck.isChecked();
		}
	}
	
	public static class EditableLayout extends LinearLayout {

		private ImageView headerIconView;
		private TextView headerTitleView;
		private LinearLayout rowLayout;
		
		public  EditableLayout(Context context) {
			super(context);
		}
		
		public  EditableLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			headerIconView = (ImageView)findViewById(R.id.form_header_icon_view);
			headerTitleView = (TextView)findViewById(R.id.form_header_title);
			rowLayout = (LinearLayout) findViewById(R.id.form_editable_row_layout);
		}	
		
		private void setMyVisibility(int visibility) {
			rowLayout.setVisibility(visibility);
			for (int i=0;i < rowLayout.getChildCount();i++) {
				rowLayout.getChildAt(i).setVisibility(visibility);
			}
		}
		
		public void close() {
			Log.d(DEBUG_TAG,"close");
			setMyVisibility(View.GONE);
		}
		
		public void open() {
			Log.d(DEBUG_TAG,"open");
			setMyVisibility(View.VISIBLE);
		}
		
		public void setTitle(PresetItem preset) {

			if (preset != null) {
				Drawable icon = preset.getIcon();
				if (icon != null) {
					headerIconView.setVisibility(View.VISIBLE);
					//NOTE directly using the icon seems to trash it, so make a copy
					headerIconView.setImageDrawable(icon.getConstantState().newDrawable()); 
				} else {
					headerIconView.setVisibility(View.GONE);
				}
				headerTitleView.setText(preset.getTranslatedName());
			} else {
				headerTitleView.setText("Unknown element (no preset)");
			}
		}
	}

	@Override
	public void tagsUpdated() {
		update();	
	}
}
