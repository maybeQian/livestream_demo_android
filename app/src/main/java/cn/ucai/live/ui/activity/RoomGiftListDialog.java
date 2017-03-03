package cn.ucai.live.ui.activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyphenate.easeui.utils.EaseUserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.ucai.live.I;
import cn.ucai.live.LiveHelper;
import cn.ucai.live.R;
import cn.ucai.live.data.model.Gift;

/**
 * Created by wei on 2016/7/25.
 */
public class RoomGiftListDialog extends DialogFragment {

    Unbinder unbinder;
    @BindView(R.id.rv_gift)
    RecyclerView rvGift;
    @BindView(R.id.tv_my_bill)
    TextView tvMyBill;
    @BindView(R.id.tv_rechrge)
    TextView tvRechrge;

    GridLayoutManager gm;
    GiftAdapter adapter;
    List<Gift> giftlist;
    private String username;

    public static RoomGiftListDialog newInstance() {
        RoomGiftListDialog dialog = new RoomGiftListDialog();
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room_gift_list, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        gm = new GridLayoutManager(getContext(), I.GIFT_COLUMN_COUNT);
        giftlist = new ArrayList<>();
        adapter = new GiftAdapter(getContext(), giftlist);
        rvGift.setAdapter(adapter);
        rvGift.setLayoutManager(gm);
        initData();
    }

    private void initData() {
        Map<Integer, Gift> giftMap = LiveHelper.getInstance().getAppGiftList();
        Iterator<Map.Entry<Integer, Gift>> iterator = giftMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Gift> next = iterator.next();
            giftlist.add(next.getValue());
        }

        Collections.sort(giftlist, new Comparator<Gift>() {
            @Override
            public int compare(Gift lhs, Gift rhs) {
                return lhs.getId().compareTo(rhs.getId());
            }
        });
        adapter.notifyDataSetChanged();
    }


    private View.OnClickListener listener;

    public void setGiftOnClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    interface UserDetailsDialogListener {
        void onMentionClick(String username);
    }

    /**
     * adapter
     */
    class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.GiftViewHolder> {
        Context context;
        List<Gift> list;

        public GiftAdapter(Context context, List<Gift> list) {
            this.context = context;
            this.list = list;
        }

        @Override
        public GiftViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View layout = View.inflate(context, R.layout.item_gift, null);
            GiftViewHolder holder = new GiftViewHolder(layout);
            return holder;
        }

        @Override
        public void onBindViewHolder(GiftViewHolder holder, int position) {
            Gift gift = list.get(position);
            holder.tvGiftName.setText(gift.getGname());
            holder.tvGiftPrice.setText(String.valueOf(gift.getGprice()));
            EaseUserUtils.setGiftAvatarByPath(context,gift.getGurl(),holder.ivGift);
            holder.itemView.setTag(gift.getId());
            holder.itemView.setOnClickListener(listener);
        }

        @Override
        public int getItemCount() {
            return list!=null?list.size():0;
        }

         class GiftViewHolder extends RecyclerView.ViewHolder{
            @BindView(R.id.iv_gift)
            ImageView ivGift;
            @BindView(R.id.tv_gift_name)
            TextView tvGiftName;
            @BindView(R.id.tv_gift_price)
            TextView tvGiftPrice;

            GiftViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 使用不带theme的构造器，获得的dialog边框距离屏幕仍有几毫米的缝隙。
        // Dialog dialog = new Dialog(getActivity());
        Dialog dialog = new Dialog(getActivity(), R.style.room_user_details_dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // must be called before set content
        dialog.setContentView(R.layout.fragment_room_user_details);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);

        // 设置宽度为屏宽、靠近屏幕底部。
        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wlp);

        return dialog;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
