package edu.gvsu.cis.activityapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import org.parceler.Parcels;

import edu.gvsu.cis.activityapp.R;
import edu.gvsu.cis.activityapp.activities.EventDetailActivity;
import edu.gvsu.cis.activityapp.util.FirebaseManager;
import edu.gvsu.cis.activityapp.util.MapManager;
import edu.gvsu.cis.activityapp.util.PlaceEvent;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class PlaceFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private FirebaseRecyclerAdapter<PlaceEvent, PlaceHolder> adapter;
    private FirebaseManager mFirebase;
    private MapManager mapManager;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlaceFragment() { }

    @SuppressWarnings("unused")
    public static PlaceFragment newInstance(int columnCount) {
        PlaceFragment fragment = new PlaceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mFirebase.getUser() != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mFirebase.getUser() != null ){
            adapter.stopListening();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mFirebase = FirebaseManager.getInstance();
        this.mapManager = MapManager.getInstance();

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place_list, container, false);

        if (this.mFirebase.getUser() != null) {
            Query keyRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid())
                    .child("groups");
            DatabaseReference valRef = FirebaseDatabase.getInstance().getReference("Places");

            FirebaseRecyclerOptions<PlaceEvent> options = new FirebaseRecyclerOptions.Builder<PlaceEvent>()
                    .setIndexedQuery(keyRef, valRef, PlaceEvent.class)
                    .build();
            if (view instanceof RecyclerView) {
                Context context = view.getContext();
                RecyclerView recyclerView = (RecyclerView) view;
                if (mColumnCount <= 1) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                } else {
                    recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
                }
                adapter = new FirebaseRecyclerAdapter<PlaceEvent, PlaceHolder>(options) {
                    @Override
                    protected void onBindViewHolder(PlaceHolder holder, int position, PlaceEvent model) {
                        holder.mItem = model;
                        holder.nameView.setText(model.getName());
                        holder.userView.setText(model.getOwner());
                        holder.mView.setOnClickListener(v -> {
                            if (null != mListener) {
                                mListener.onListFragmentInteraction(holder.mItem);
                            }
                        });

                        if (model.getPlaceId() != null) {
                            mapManager.getPlacePhoto(model.getPlaceId()).addOnCompleteListener((task) -> {
                                if (task.isSuccessful() && task.getResult().getPhotoMetadata().getCount() > 0) {
                                    PlacePhotoMetadata metadata = task.getResult().getPhotoMetadata().get(0);
                                    mapManager.getBitmapPhoto(metadata).addOnCompleteListener((photoTask) -> {
                                        if (photoTask.isSuccessful()) {
                                            holder.imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                                            holder.imageView.setImageBitmap(photoTask.getResult().getBitmap());
                                        }
                                    });
                                }
                            });
                        }

                    }

                    @Override
                    public PlaceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.fragment_place, parent, false);
                        return new PlaceHolder(view);
                    }
                };
                recyclerView.setAdapter(adapter);
            }
        }

        return view;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(PlaceEvent item);
    }

    public class PlaceHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final ImageView imageView;
        public final TextView nameView;
        public final TextView userView;
        public PlaceEvent mItem;

        public PlaceHolder(View view) {
            super(view);
            mView = view;
            imageView = (ImageView) view.findViewById(R.id.event_image);
            nameView = (TextView) view.findViewById(R.id.event_name);
            userView = (TextView) view.findViewById(R.id.event_user_name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + userView.getText() + "'";
        }
    }
}
