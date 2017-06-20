package pavel11.rectangledetection;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.util.ArrayList;

import static java.security.AccessController.getContext;

public class PersonalAdapter extends RecyclerView.Adapter<PersonalAdapter.ListViewHolder> {

    private static ArrayList<Rettangolo> items;

    public PersonalAdapter(ArrayList<Rettangolo> items){
        this.items=items;
//        this.items.clear();
    }

    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemLayoutView= LayoutInflater.from(parent.getContext()).inflate(R.layout.list_layout,null);
        ListViewHolder viewHolder= new ListViewHolder(itemLayoutView);
        return  viewHolder;
    }

    @Override
    public void onBindViewHolder(final ListViewHolder viewHolder, int position) {
        viewHolder.textView.setText((CharSequence) items.get(position).getNomeFoto());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class  ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView textView;
        Context context;


        public ListViewHolder(View itemView) {
            super(itemView);
            context=itemView.getContext();
            textView=(TextView)itemView.findViewById(R.id.foto_name);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String nameMarker=textView.getText().toString();
                    Intent openSeeRectActivity = new Intent(context,SeeRectActivity.class);
                    context.startActivity(openSeeRectActivity.putExtra("luogo",nameMarker));
                }
            });
        }

        @Override
        public void onClick(View v) {
            int p=getAdapterPosition();
        }
    }
}

