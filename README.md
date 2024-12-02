# Android_mid
安卓期中作业
具体代码见master分支

## （1）时间戳
### 除了完成必须的时间戳添加外，还修改了列表的形式
第一行为标题，第二行为笔记的部分正文，第三行为时间戳，列表右侧显示笔记的分类
![微信图片_20241201234810](https://github.com/user-attachments/assets/2d723a2b-911e-43e7-b89b-fed0147c012c)

## 编辑页的整体形式如下
![微信图片_20241202002227](https://github.com/user-attachments/assets/d543f507-8c90-41a8-b8cd-37c9d980e7e9)


## （2）查询
新建了一个select_by_title_or_content.xml文件，创建实现查询功能的对话框

修改了NoteList类中的onOptionsItemSelected，处理menu_select菜单项的点击事件
~~~
case R.id.menu_select:
                LayoutInflater flater = LayoutInflater.from(this);
                View view = flater.inflate(R.layout.select_by_title_or_content, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(view);
                final AlertDialog alert = builder.create();
                Button btn_select = (Button)view.findViewById(R.id.btn_select);
                final EditText content=(EditText)view.findViewById(R.id.content);
                btn_select.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        String s = String.valueOf(content.getText()).trim();
                        if (TextUtils.isEmpty(s)||"null".equals(s)){
                            Toast.makeText(NotesList.this, "内容为空，请输入内容", Toast.LENGTH_SHORT).show();
                            editor.putString("res", null);
                        }else {
                            editor.putString("res", s);
                        }
                        editor.apply();
                        cursor = managedQuery(
                                data,
                                PROJECTION,
                                null,
                                null,
                                NotePad.Notes.DEFAULT_SORT_ORDER
                        );

                        adapter = new SimpleCursorAdapter(
                                v.getContext(),
                                R.layout.noteslist_item,
                                cursor,
                                dataColumns,
                                viewIDs
                        );
                        setListAdapter(adapter);
                        alert.cancel();
                    }
                });
                alert.show();
                return true;
~~~
### 点击右上角的放大镜，能实现查询功能
![微信图片_20241201235531](https://github.com/user-attachments/assets/c59f79d2-d0eb-4a1f-915b-ed2a49194d96)
### 能返回匹配的内容
![微信图片_20241201235537](https://github.com/user-attachments/assets/33ea7137-6cfd-45c6-8a2e-d2b4b7d7e584)
### 当输入为空时，能提示用户
![微信图片_20241201235543](https://github.com/user-attachments/assets/f5ed6b08-82ee-43cd-848e-c6eb824b84c3)

## （3）更改背景色
### 在笔记编辑页面，能通过按钮改变背景色
![微信图片_20241202000929](https://github.com/user-attachments/assets/8205bb14-a7e6-46a2-9221-45c9ebb08178)
### 选择背景色
新建了一个ColorPickerDialog 的类，创建一个用于颜色选择的对话框
~~~
public class ColorPickerDialog extends DialogFragment {
    private final String[] colors = {"#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF"};

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("选择背景颜色")
                .setItems(colors, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 此处可以处理用户选择的颜色
                        String selectedColor = colors[which];
                        // TODO: 处理选中的颜色
                    }
                });
        return builder.create();
    }
}
~~~

在NoteEditor类中，设置按钮点击事件
~~~
sharedPreferences = getSharedPreferences("image", Context.MODE_PRIVATE);
int bgColor = sharedPreferences.getInt("bg_color", R.color.wihte);
all.setBackgroundResource(bgColor);
btn = (Button) findViewById(R.id.btn);
btn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        LayoutInflater flater = LayoutInflater.from(view.getContext());
        View v = flater.inflate(R.layout.my_color_select, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setView(v);
        final AlertDialog dialog = builder.create();
        
        Button red = (Button) v.findViewById(R.id.red);
        red.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("bg_color", R.color.red);
                editor.apply(); // 应用更改
                dialog.cancel();
            }
        });
     
        dialog.show();
    }
});
~~~

![微信图片_20241202000921](https://github.com/user-attachments/assets/339b0340-988a-450d-9588-a87ffd3403c1)
### 可以看到背景颜色改变
![微信图片_20241202000934](https://github.com/user-attachments/assets/5832a7a6-7c76-4ee1-b3fd-23e038761ba7)

## （3）笔记分类
### 在笔记编辑界面，可以设计笔记的类别
![微信图片_20241202001213](https://github.com/user-attachments/assets/bde3aaca-f0c9-4b4c-8c33-fd37996038d5)

## （4）图片保存
### 笔记下方支持保存图片，读取相册内的照片
![微信图片_20241202001804](https://github.com/user-attachments/assets/ead61129-ccf5-4d5d-83fa-309e05736bf0)
