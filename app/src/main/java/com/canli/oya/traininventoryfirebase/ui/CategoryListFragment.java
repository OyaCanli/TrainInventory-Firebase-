package com.canli.oya.traininventoryfirebase.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.canli.oya.traininventoryfirebase.R;
import com.canli.oya.traininventoryfirebase.adapters.CategoryAdapter;
import com.canli.oya.traininventoryfirebase.databinding.FragmentListBinding;
import com.canli.oya.traininventoryfirebase.repositories.CategoryRepository;
import com.canli.oya.traininventoryfirebase.utils.Constants;
import com.canli.oya.traininventoryfirebase.viewmodel.MainViewModel;
import com.firebase.ui.auth.AuthUI;

import java.util.List;

import timber.log.Timber;

public class CategoryListFragment extends Fragment implements CategoryAdapter.CategoryItemClickListener,
        CategoryRepository.CategoryUseListener {

    private CategoryAdapter mAdapter;
    private List<String> mCategories;
    private FragmentListBinding binding;
    private MainViewModel mViewModel;
    private CoordinatorLayout coordinator;
    private String categoryToErase;
    private final Handler handler = new Handler();
    private boolean mIsEmpty;
    private Runnable setEmpty = new Runnable() {
        @Override
        public void run() {
            Timber.d("Runnable setEmpty is executed");
            if (mIsEmpty) {
                binding.included.setIsEmpty(true);
                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.translate_from_left);
                binding.included.emptyImage.startAnimation(animation);
            }
        }
    };

    public CategoryListFragment() {
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_list, container, false);

        setHasOptionsMenu(true);

        mAdapter = new CategoryAdapter(this);

        binding.included.list.setLayoutManager(new LinearLayoutManager(getActivity()));
        binding.included.list.setItemAnimator(new DefaultItemAnimator());
        binding.included.list.setAdapter(mAdapter);
        binding.included.setIsLoading(true);
        binding.included.setIsEmpty(false);

        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTitle(getString(R.string.all_categories));
        coordinator = getActivity().findViewById(R.id.coordinator);
        mViewModel = ViewModelProviders.of(getActivity()).get(MainViewModel.class);
        mViewModel.initializeCategoryRepo(this);
        mViewModel.getCategoryList().observe(getActivity(), new Observer<List<String>>() {
            @Override
            public void onChanged(@Nullable List<String> categoryEntries) {
                if (categoryEntries != null) {
                    Timber.d("onChange is called,data is not null");
                    binding.included.setIsLoading(false);
                    if (categoryEntries.isEmpty()) {
                        Timber.d("onChange is called, list is empty");
                        binding.included.setEmptyMessage(getString(R.string.no_categories_found));
                        setIsEmpty(true);
                        handler.postDelayed(setEmpty, 300);
                    } else {
                        setIsEmpty(false);
                        Timber.d("onChange is called, list is not empty");
                        binding.included.emptyImage.clearAnimation();
                        mAdapter.setCategories(categoryEntries);
                        mCategories = categoryEntries;
                        binding.included.setIsEmpty(false);
                    }
                } else {
                    Timber.d("onChange is called but data is null");
                }
            }
        });

        //This part is for providing swipe-to-delete functionality, as well as a snack bar to undo deleting
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();

                //First take a backup of the category to erase
                categoryToErase = mCategories.get(position);

                //Check whether this category is used by trains table
                mViewModel.checkIfCategoryUsed(categoryToErase);
            }
        }).attachToRecyclerView(binding.included.list);
    }

    private void setIsEmpty(boolean isEmpty) {
        mIsEmpty = isEmpty;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_add_item, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_add) {
            openAddCategoryFragment();
        } else if (itemId == R.id.sign_out) {
            AuthUI.getInstance().signOut(getActivity());
        }
        return super.onOptionsItemSelected(item);
    }

    private void openAddCategoryFragment() {
        AddCategoryFragment addCatFrag = new AddCategoryFragment();
        getChildFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.translate_from_top, 0)
                .replace(R.id.brandlist_addFrag_container, addCatFrag)
                .commit();
    }

    @Override
    public void onCategoryItemClicked(String categoryName) {
        TrainListFragment trainListFrag = new TrainListFragment();
        Bundle args = new Bundle();
        args.putString(Constants.INTENT_REQUEST_CODE, Constants.TRAINS_OF_CATEGORY);
        args.putString(Constants.CATEGORY_NAME, categoryName);
        trainListFrag.setArguments(args);
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.container, trainListFrag)
                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onCategoryUseCaseReturned(boolean isCategoryUsed) {
        if (isCategoryUsed) {
            //IF category is used, warn the user and don't erase it.
            Toast.makeText(getActivity(), R.string.cannot_erase_category, Toast.LENGTH_LONG).show();
            mAdapter.notifyDataSetChanged();
        } else {
            deleteCategory();
        }
    }

    private void deleteCategory() {
        mViewModel.deleteCategory(categoryToErase);
        //Show a snack bar for undoing delete
        Snackbar snackbar = Snackbar
                .make(coordinator, R.string.category_deleted, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //If UNDO is clicked, add the item back in the database
                        mViewModel.insertCategory(categoryToErase);
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .setActionTextColor(getResources().getColor(R.color.window_background));
        snackbar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        snackbar.show();
    }
}
