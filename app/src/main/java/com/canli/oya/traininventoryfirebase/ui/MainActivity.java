package com.canli.oya.traininventoryfirebase.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.canli.oya.traininventoryfirebase.R;
import com.canli.oya.traininventoryfirebase.databinding.ActivityMainBinding;
import com.canli.oya.traininventoryfirebase.utils.Constants;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener,
        AddTrainFragment.UnsavedChangesListener, FragmentManager.OnBackStackChangedListener {

    private ActivityMainBinding binding;
    private boolean thereAreUnsavedChanges;
    private FragmentManager fm;
    private TrainListFragment mTrainListFragment;
    private BrandListFragment mBrandListFragment;
    private CategoryListFragment mCategoryListFragment;
    private FirebaseAuth mFirebaseAuth;
    private static final String TAG = "MainActivity";
    public static final int RC_SIGN_IN = 1;
    private FirebaseAuth.AuthStateListener mAuthStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                Log.d(TAG, "user null, starting sign in screen");
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build(),
                                        new AuthUI.IdpConfig.GoogleBuilder().build()))
                                .build(),
                        RC_SIGN_IN);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
       mFirebaseAuth.addAuthStateListener(mAuthStateListener);

        //Set toolbar
        setSupportActionBar(binding.toolbar);

        binding.navigation.setOnNavigationItemSelectedListener(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        fm = getSupportFragmentManager();

        if(savedInstanceState == null){
            fm.beginTransaction()
                    .setCustomAnimations(0, android.R.animator.fade_out)
                    .add(R.id.container, getCategoryListFragment())
                    .commit();
        }
    }

    /*@Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume is called");
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }*/

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        FragmentTransaction ft = fm.beginTransaction();
        Fragment currentFrag = fm.findFragmentById(R.id.container);

        int id = item.getItemId();
        switch (id) {
            case R.id.trains: {
                if (currentFrag instanceof TrainListFragment) {
                    ((TrainListFragment) currentFrag).scrollToTop();
                    break;
                }
                TrainListFragment trainListFrag = getTrainListFragment();
                Bundle args = new Bundle();
                args.putString(Constants.INTENT_REQUEST_CODE, Constants.ALL_TRAIN);
                trainListFrag.setArguments(args);
                ft.replace(R.id.container, trainListFrag);
                break;
            }
            case R.id.brands: {
                if (currentFrag instanceof BrandListFragment) {
                    break;
                }
                ft.replace(R.id.container, getBrandListFragment());
                break;
            }
            case R.id.categories: {
                if (currentFrag instanceof CategoryListFragment) {
                    break;
                }
                ft.replace(R.id.container, getCategoryListFragment());
                break;
            }
        }
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .addToBackStack(null)
                .commit();
        return true;
    }

    private void hideOrShowBottomNavigation(Fragment currentFrag) {
        if (currentFrag instanceof AddTrainFragment) {
            binding.navigation.setVisibility(View.GONE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            binding.navigation.setVisibility(View.VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void clearFocusAndHideKeyboard() {
        //This is for closing soft keyboard if user navigates to another fragment while keyboard was open
        View focusedView = this.getCurrentFocus();
        if (focusedView != null) {
            focusedView.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    public void setMenuItemChecked(Fragment currentFrag) {
        /*If user navigates with back button, active menu item doesn't adapt itself.
        We need to set it checked programmatically.*/
        if (currentFrag instanceof BrandListFragment) {
            binding.navigation.getMenu().getItem(1).setChecked(true);
        } else if (currentFrag instanceof CategoryListFragment) {
            binding.navigation.getMenu().getItem(0).setChecked(true);
        } else {
            binding.navigation.getMenu().getItem(2).setChecked(true);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.container);
        if (thereAreUnsavedChanges && currentFrag instanceof AddTrainFragment) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showUnsavedChangesDialog() {
        //If user clicks back when there are unsaved changes in AddTrainFragment, warn user with a dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_warning);
        builder.setPositiveButton(getString(R.string.discard_changes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Changes will be discarded
                thereAreUnsavedChanges = false;
                onBackPressed();
            }
        });
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void warnForUnsavedChanges(boolean shouldWarn) {
        thereAreUnsavedChanges = shouldWarn;
    }

    @Override
    public void onBackStackChanged() {
        clearFocusAndHideKeyboard();
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.container);
        setMenuItemChecked(currentFrag);
        hideOrShowBottomNavigation(currentFrag);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult is called");
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "result ok");
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "result cancelled");
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public TrainListFragment getTrainListFragment() {
        if (mTrainListFragment == null) {
            mTrainListFragment = new TrainListFragment();
        }
        return mTrainListFragment;
    }

    public BrandListFragment getBrandListFragment() {
        if (mBrandListFragment == null) {
            mBrandListFragment = new BrandListFragment();
        }
        return mBrandListFragment;
    }

    public CategoryListFragment getCategoryListFragment() {
        if (mCategoryListFragment == null) {
            mCategoryListFragment = new CategoryListFragment();
        }
        return mCategoryListFragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean(Constants.UNSAVED_CHANGES, thereAreUnsavedChanges);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }
}
