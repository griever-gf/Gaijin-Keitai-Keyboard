/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.daicon.griever.gaijinkeitaiboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
//import android.text.method.MetaKeyKeyListener;
import android.text.method.MultiTapKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.os.Handler;
import android.app.Notification;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.app.PendingIntent;
import android.content.Intent;
//import android.os.Bundle;
//import android.app.Activity;
//import android.support.v4.app.NotificationCompat.Builder;
import android.app.PendingIntent;



import com.daicon.griever.gaijinkeitaiboard.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class GaijinKeitaiKeyboard extends InputMethodService {
    
    //private StringBuilder mComposing = new StringBuilder();
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;
    
    char[][] lang_rus = new char[][]{
    		  { ' ', '0', '+', '-', '_' },
    		  { '.', ',', '1', '?', '!', '@', '#', '$', '%', '^', '&', '*', ':', '/', '\'', '=', '(', ')' },
    		  { '�', '�', '�', '�', '2', 'A', '�', '�', '�' },
    		  { '�', '�', '�', '�', '�', '3', '�', '�', '�', '�', '�',},
    		  { '�', '�', '�', '�', '4'},
    		  { '�', '�', '�', '�', '5'},
    		  { '�', '�', '�', '�', '6'},
    		  { '�', '�', '�', '�', '7'},
    		  { '�', '�', '�', '�', '8'},
    		  { '�', '�', '�', '�', '9'},
    		  { '*' },
    		  { '#' }
    		};
    
    private int currentKeyCharIndex;
    private int lastKeyCode;
    
    private Handler mHandler;
    
    //private LatinKeyboard mSymbolsKeyboard;
    //private LatinKeyboard mSymbolsShiftedKeyboard;
    //private LatinKeyboard mQwertyKeyboard;
    
    //private LatinKeyboard mCurKeyboard;/
    
    private String mWordSeparators;
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        android.os.Debug.waitForDebugger();  // DEBUG MODE
        mWordSeparators = getResources().getString(R.string.word_separators);
        mHandler = new Handler();
    }
    
    
    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        //mComposing.setLength(0);
        //updateCandidates();
        currentKeyCharIndex = 0;
        lastKeyCode = 0;
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        //mPredictionOn = false;
        //mCompletionOn = false;
        //mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                //mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case EditorInfo.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                //mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                //mCurKeyboard = mQwertyKeyboard;
                //mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    //mPredictionOn = false;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    //mPredictionOn = false;
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    //mPredictionOn = false;
                    //mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                //mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        //---get the notification ID for the notification; 
        // passed in by the MainActivity---
        
         
        //Notification notif = new Notification( R.drawable.lang_icon_ru,  "Time's up!", System.currentTimeMillis());
        //should add custom tag later
        //nm.notify(99, notif);
        try
        {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, GaijinKeitaiKeyboard.class);
        NotificationCompat.Builder mBuilder =
        	    new NotificationCompat.Builder(this)
        	    .setSmallIcon(R.drawable.lang_icon_ru)
        	    .setContentTitle("My notification")
        	    .setContentText("Hello World!")
        	    .setContentIntent(PendingIntent.getActivity(this, 0, notificationIntent, 0));

     // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        ////TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        ////stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        ////stackBuilder.addNextIntent(resultIntent);
        
        ////PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    ////0, PendingIntent.FLAG_UPDATE_CURRENT);
        nm.notify(5954, mBuilder.build());
        }
        catch (Exception e)
        {
        	String s = e.getMessage();
        }
        // Update the label on the enter key, depending on what the application
        // says it will do.
        //mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        //mComposing.setLength(0);
        //updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        //setCandidatesViewShown(false);
        
        //mCurKeyboard = mQwertyKeyboard;
        //if (mInputView != null) {
            //mInputView.closing();
        //}
    }
    
    //@Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        //super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        //mInputView.setKeyboard(mCurKeyboard);
        //mInputView.closing();
    //}
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    /*@Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }*/ 
    
    Runnable postEditedCharacter = new Runnable() {
        public void run() {
        	currentKeyCharIndex = 0;
        	lastKeyCode = 0;
        	getCurrentInputConnection().finishComposingText();
        }
      };
   
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
	        case KeyEvent.KEYCODE_0: //7
	        case KeyEvent.KEYCODE_1: //8
	        case KeyEvent.KEYCODE_2: //...
	        case KeyEvent.KEYCODE_3:
	        case KeyEvent.KEYCODE_4:
	        case KeyEvent.KEYCODE_5:
	        case KeyEvent.KEYCODE_6:
	        case KeyEvent.KEYCODE_7:
	        case KeyEvent.KEYCODE_8:
	        case KeyEvent.KEYCODE_9: //16
	        case KeyEvent.KEYCODE_STAR: //17
	        case KeyEvent.KEYCODE_POUND: //18
	        	mHandler.removeCallbacks(postEditedCharacter);

	        	if ((keyCode != lastKeyCode) && (lastKeyCode != 0)) //if edit in progress, but new key is pressed
	        	{ 
	        		//post previous char
	            	getCurrentInputConnection().finishComposingText();
	            	currentKeyCharIndex = 0;
	        	}
	        	
	        	//post delayed current char
        		getCurrentInputConnection().setComposingText(String.valueOf(lang_rus[keyCode-7][currentKeyCharIndex]), 1);
	            mHandler.postDelayed(postEditedCharacter, 700);
	            
	            //currentKeyCharIndex++
	            currentKeyCharIndex = (currentKeyCharIndex + 1) % lang_rus[keyCode-7].length;
            	lastKeyCode = keyCode;

	        	return true;
        
	        default:
	        	break;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override public boolean onKeyLongPress(int keyCode, KeyEvent event)
    {
    	int i = keyCode;
    	return super.onKeyLongPress(keyCode, event);
    }
    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.   */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
    	mMetaState = MultiTapKeyListener.handleKeyDown(mMetaState,keyCode, event);
    	if (event.getRepeatCount() > 2)
    	{
    	int xxx = 666;
    	}
    	int c = event.getUnicodeChar(MultiTapKeyListener.getMetaState(mMetaState)); //0 if inactive, 1 if active, 2 if locked.
        mMetaState = MultiTapKeyListener.adjustMetaAfterKeypress(mMetaState);
        
        InputConnection ic = getCurrentInputConnection(); 
        //if no input connection or char code is 0
        if (c == 0 || ic == null) {
            return false;
        }
        
        boolean dead = false;
        //dead key check
        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        
        /*if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }*/
        onKey(c, null);
        return true;
    }
    
    public void onKey(int primaryCode, int[] keyCodes) {
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            //if (mComposing.length() > 0) {
                //commitTyped(getCurrentInputConnection());
            //}
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        //} else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        //}
        //else
        	//if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                //&& mInputView != null
                //) {
            //Keyboard current = mInputView.getKeyboard();
            //if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                //current = mQwertyKeyboard;
            //} else {
                //current = mSymbolsKeyboard;
            //}
            //mInputView.setKeyboard(current);
            //if (current == mSymbolsKeyboard) {
                //current.setShifted(false);
            //}
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
    	
    	//InputMethodManager.this.setInputMethod(token, id)
    	//int xxx = KeyCharacterMap.getKeyboardType();
    	//getCurrentInputConnection().
    	
        if (isInputViewShown()) {
            //if (mInputView.isShifted())
            {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        android.os.Debug.waitForDebugger();
        /*if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateShiftKeyState(getCurrentInputEditorInfo());
            //updateCandidates();
        } else*/ 
        {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
    }


    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        //if (PROCESS_HARD_KEYS) {
            //if (mPredictionOn) {
                //mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        //keyCode, event);
            //}
        //}
    	//lastKeyCode = 0;
        
        return super.onKeyUp(keyCode, event);
    }
    
    //this is should work for unicode keys, so basically unusable
    @Override public boolean onKeyMultiple(int keyCode, int count, KeyEvent event)
    {
    	 android.os.Debug.waitForDebugger();  // DEBUG MODE
    	 int newKeyCode = keyCode;
         if ( (keyCode == KeyEvent.KEYCODE_BACK) )
         {
             newKeyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
         }
        return super.onKeyMultiple(newKeyCode, count, event);
    	//return super.onKeyMultiple(keyCode, count, event);
    }
    

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    /*private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            //updateCandidates();
        }
    }*/

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                //&& mInputView != null
                //&& mQwertyKeyboard == mInputView.getKeyboard()
                ) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            //mInputView.setShifted(mCapsLock || caps != 0);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }
    
    /*public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }*/

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    /*private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        //if (mCandidateView != null) {
            //mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        //}
    }*/
    
    private void handleBackspace() {
    	final int length = getCurrentInputConnection().getTextBeforeCursor(1, 0).length();
    	if (length > 0) {
            //getCurrentInputConnection().commitText();
            //updateCandidates();
    		getCurrentInputConnection().deleteSurroundingText(1, 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        /*final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            //updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            //updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }*/
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        /*if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }*/
    }
   

    private void handleClose() {
        //commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        //mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        //pickSuggestionManually(0);
    }
    
    /*public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            //if (mCandidateView != null) {
                //mCandidateView.clear();
            //}
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }*/
    
    public void swipeRight() {
        //if (mCompletionOn) {
            //pickDefaultCandidate();
        //}
    }
    
    public void swipeLeft() {
        //handleBackspace();
    }

    public void swipeDown() {
        //handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }
}
