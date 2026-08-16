package com.seyma.ofis;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.database.sqlite.*;
import android.database.Cursor;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {

    DB db;
    LinearLayout content, nav;
    TextView title;
    int pad = 16;
    int dark = Color.rgb(16,24,32);
    int gold = Color.rgb(201,162,39);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        setContentView(R.layout.activity_main);

        db = new DB(this);
        content = findViewById(R.id.content);
        nav = findViewById(R.id.nav);
        title = findViewById(R.id.title);

        buildNav();
        dashboard();
    }

    TextView tv(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(dark);
        t.setPadding(pad,pad,pad,pad);
        return t;
    }

    Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    void clear(String name) {
        content.removeAllViews();
        title.setText(name);
    }

    void buildNav() {
        String[] a = {
            "Ana Sayfa",
            "Müşteriler",
            "Siparişler",
            "Cari",
            "Kasa",
            "Raporlar"
        };

        for (String s : a) {
            Button b = btn(s, v -> {
                if (s.equals("Ana Sayfa"))
                    dashboard();
                else if (s.equals("Müşteriler"))
                    customers();
                else if (s.equals("Siparişler"))
                    orders();
                else if (s.equals("Cari"))
                    ledger();
                else if (s.equals("Kasa"))
                    cash();
                else
                    reports();
            });

            nav.addView(b);
        }
    }

    void dashboard() {
        clear("ŞEYMAŞ OFİS");

        TextView h = tv(
            "Ticari Yönetim Paneli\nŞeymaş Kapı – Erzurum",
            22
        );

        h.setTextColor(dark);
        h.setTypeface(null,1);
        content.addView(h);

        double receivable = db.totalReceivable();
        double sales = db.totalSales();
        double cash = db.totalCash();
        double expense = db.totalExpense();

        addCard("Toplam Ciro", money(sales));
        addCard("Toplam Alacak", money(receivable));
        addCard("Net Kasa", money(cash-expense));
        addCard("Toplam Gider", money(expense));

        content.addView(tv("Hızlı İşlemler",18));

        content.addView(
            btn("+ Yeni Müşteri",v -> customerDialog())
        );

        content.addView(
            btn("+ Yeni Sipariş",v -> orderDialog())
        );

        content.addView(
            btn("+ Tahsilat",v -> paymentDialog())
        );

        content.addView(
            btn("+ Gider",v -> expenseDialog())
        );
    }

    void addCard(String a,String b) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18,10,18,10);
        c.setBackgroundColor(Color.WHITE);

        c.addView(tv(a,14));

        TextView x = tv(b,24);
        x.setTypeface(null,1);

        c.addView(x);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);

        p.setMargins(0,6,0,6);

        content.addView(c,p);
    }

    String money(double x) {
        return String.format(
            Locale.US,
            "%,.2f TL",
            x
        )
        .replace(',', 'X')
        .replace('.', ',')
        .replace('X','.');
    }

    void customers() {
        clear("Müşteriler");

        content.addView(
            btn("+ Müşteri Ekle",v -> customerDialog())
        );

        Cursor c = db.q(
            "select id,name,phone,address " +
            "from customers order by id desc"
        );

        while(c.moveToNext()) {

            String s =
                c.getString(1) +
                "\nTel: " +
                c.getString(2) +
                "\n" +
                c.getString(3);

            content.addView(tv(s,16));

            content.addView(
                btn(
                    "Müşteri Cari Detayı",
                    v -> customerLedger(
                        c.getLong(0),
                        c.getString(1)
                    )
                )
            );
        }

        c.close();
    }

    void customerDialog() {

        final LinearLayout f = form();

        EditText n = field("Müşteri adı *");
        EditText p = field("Telefon");
        EditText a = field("Adres");
        EditText tax = field("Vergi no");
        EditText note = field("Not");

        f.addView(n);
        f.addView(p);
        f.addView(a);
        f.addView(tax);
        f.addView(note);

        new AlertDialog.Builder(this)
            .setTitle("Yeni Müşteri")
            .setView(f)
            .setPositiveButton(
                "Kaydet",
                (d,w) -> {

                    db.exec(
                        "insert into customers" +
                        "(name,phone,address,tax,note) " +
                        "values(?,?,?,?,?)",

                        n.getText().toString(),
                        p.getText().toString(),
                        a.getText().toString(),
                        tax.getText().toString(),
                        note.getText().toString()
                    );

                    customers();
                }
            )
            .setNegativeButton("İptal",null)
            .show();
    }

    void orders() {

        clear("Siparişler");

        content.addView(
            btn("+ Yeni Sipariş",v -> orderDialog())
        );

        Cursor c = db.q(
            "select o.id,c.name,o.kind,o.width," +
            "o.height,o.price,o.paid,o.status " +
            "from orders o " +
            "left join customers c " +
            "on c.id=o.customer_id " +
            "order by o.id desc"
        );

        while(c.moveToNext()) {

            double price = c.getDouble(5);
            double paid = c.getDouble(6);

            String s =
                "#" + c.getLong(0) +
                " • " + c.getString(1) +
                "\n" +
                c.getString(2) +
                " | " +
                c.getInt(3) +
                " x " +
                c.getInt(4) +
                " mm\n" +
                "Fiyat: " +
                money(price) +
                " | Kapora: " +
                money(paid) +
                " | Kalan: " +
                money(price-paid) +
                "\nDurum: " +
                c.getString(7);

            content.addView(tv(s,15));
        }

        c.close();
    }

    void orderDialog() {

        final LinearLayout f = form();

        Spinner cust = new Spinner(this);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Long> ids = new ArrayList<>();

        Cursor cc = db.q(
            "select id,name from customers order by name"
        );

        while(cc.moveToNext()) {
            ids.add(cc.getLong(0));
            names.add(cc.getString(1));
        }

        cc.close();

        if(names.size() == 0) {
            Toast.makeText(
                this,
                "Önce müşteri ekleyin",
                Toast.LENGTH_LONG
            ).show();

            return;
        }

        cust.setAdapter(
            new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
        );

        f.addView(cust);

        EditText kind =
            field("Sipariş türü (Çelik Kapı / Bina Giriş)");

        EditText w = field("En (mm)");
        EditText h = field("Yükseklik (mm)");
        EditText kas = field("Kasa (mm)");
        EditText open = field("Açılış yönü");
        EditText lock = field("Kilit");
        EditText mat = field("Malzeme / model");
        EditText price = field("Toplam fiyat TL");
        EditText paid = field("Kapora TL");
        EditText status = field("Durum");
        EditText note = field("Not");

        for(EditText e :
            new EditText[]{
                kind,w,h,kas,open,lock,
                mat,price,paid,status,note
            }
        )
            f.addView(e);

        new AlertDialog.Builder(this)
            .setTitle("Yeni Sipariş")
            .setView(f)
            .setPositiveButton(
                "Kaydet",
                (d,x) -> {

                    db.exec(
                        "insert into orders(" +
                        "customer_id,kind,width,height,case_mm," +
                        "opening,lock,material,price,paid,status," +
                        "note,created) " +
                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?)",

                        ids.get(
                            cust.getSelectedItemPosition()
                        ),

                        kind.getText().toString(),
                        num(w),
                        num(h),
                        num(kas),
                        open.getText().toString(),
                        lock.getText().toString(),
                        mat.getText().toString(),
                        moneyVal(price),
                        moneyVal(paid),
                        status.getText().toString(),
                        note.getText().toString(),
                        now()
                    );

                    orders();
                }
            )
            .setNegativeButton("İptal",null)
            .show();
    }

    LinearLayout form() {

        LinearLayout l = new LinearLayout(this);

        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(12,4,12,4);

        return l;
    }

    EditText field(String hint) {

        EditText e = new EditText(this);
        e.setHint(hint);

        return e;
    }

    int num(EditText e) {

        try {
            return Integer.parseInt(
                e.getText().toString()
            );
        }
        catch(Exception x) {
            return 0;
        }
    }

    double moneyVal(EditText e) {

        try {
            return Double.parseDouble(
                e.getText()
                .toString()
                .replace(".","")
                .replace(",",".")
            );
        }
        catch(Exception x) {
            return 0;
        }
    }

    String now() {

        return new SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            Locale.getDefault()
        ).format(new Date());
    }

    void ledger() {

        clear("Cari / Alacaklar");

        addCard(
            "Toplam Alacak",
            money(db.totalReceivable())
        );

        content.addView(
            btn("+ Tahsilat",v -> paymentDialog())
        );

        Cursor c = db.q(
            "select c.id,c.name," +
            "coalesce(sum(o.price-o.paid),0) due " +
            "from customers c " +
            "left join orders o " +
            "on o.customer_id=c.id " +
            "group by c.id " +
            "order by due desc"
        );

        while(c.moveToNext()) {

            content.addView(
                tv(
                    c.getString(1) +
                    "\nKalan borç: " +
                    money(c.getDouble(2)),
                    17
                )
            );
        }

        c.close();
    }

    void customerLedger(long id,String name) {

        clear(name+" – Cari");

        Cursor c = db.q(
            "select amount,method,description,created " +
            "from payments " +
            "where customer_id="+id+
            " order by id desc"
        );

        double total = 0;

        while(c.moveToNext()) {

            total += c.getDouble(0);

            content.addView(
                tv(
                    "Tahsilat: " +
                    money(c.getDouble(0)) +
                    " | " +
                    c.getString(1) +
                    "\n" +
                    c.getString(2) +
                    "\n" +
                    c.getString(3),
                    15
                )
            );
        }

        c.close();

        content.addView(
            tv(
                "Toplam tahsilat: " +
                money(total),
                18
            )
        );
    }

    void paymentDialog() {

        final LinearLayout f = form();

        Spinner cust = new Spinner(this);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Long> ids = new ArrayList<>();

        Cursor c = db.q(
            "select id,name from customers order by name"
        );

        while(c.moveToNext()) {
            ids.add(c.getLong(0));
            names.add(c.getString(1));
        }

        c.close();

        if(names.size() == 0) {

            Toast.makeText(
                this,
                "Önce müşteri ekleyin",
                Toast.LENGTH_LONG
            ).show();

            return;
        }

        cust.setAdapter(
            new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                names
            )
        );

        f.addView(cust);

        EditText amt =
            field("Tahsilat tutarı TL");

        EditText method =
            field("Ödeme yöntemi");

        EditText desc =
            field("Açıklama");

        f.addView(amt);
        f.addView(method);
        f.addView(desc);

        new AlertDialog.Builder(this)
            .setTitle("Tahsilat / Kısmi Ödeme")
            .setView(f)
            .setPositiveButton(
                "Kaydet",
                (d,w) -> {

                    db.exec(
                        "insert into payments(" +
                        "customer_id,amount,method," +
                        "description,created) " +
                        "values(?,?,?,?,?)",

                        ids.get(
                            cust.getSelectedItemPosition()
                        ),

                        moneyVal(amt),
                        method.getText().toString(),
                        desc.getText().toString(),
                        now()
                    );

                    ledger();
                }
            )
            .setNegativeButton("İptal",null)
            .show();
    }

    void cash() {

        clear("Kasa / Giderler");

        addCard(
            "Tahsilatlar",
            money(db.totalCash())
        );

        addCard(
            "Giderler",
            money(db.totalExpense())
        );

        content.addView(
            btn("+ Gider Ekle",v -> expenseDialog())
        );

        Cursor c = db.q(
            "select title,amount,created " +
            "from expenses order by id desc"
        );

        while(c.moveToNext()) {

            content.addView(
                tv(
                    c.getString(0) +
                    "\n" +
                    money(c.getDouble(1)) +
                    " • " +
                    c.getString(2),
                    15
                )
            );
        }

        c.close();
    }

    void expenseDialog() {

        final LinearLayout f = form();

        EditText t =
            field("Gider açıklaması");

        EditText a =
            field("Tutar TL");

        f.addView(t);
        f.addView(a);

        new AlertDialog.Builder(this)
            .setTitle("Gider Ekle")
            .setView(f)
            .setPositiveButton(
                "Kaydet",
                (d,w) -> {

                    db.exec(
                        "insert into expenses" +
                        "(title,amount,created) " +
                        "values(?,?,?)",

                        t.getText().toString(),
                        moneyVal(a),
                        now()
                    );

                    cash();
                }
            )
            .setNegativeButton("İptal",null)
            .show();
    }

    void reports() {

        clear("Raporlar");

        addCard(
            "Ciro",
            money(db.totalSales())
        );

        addCard(
            "Tahsilat",
            money(db.totalCash())
        );

        addCard(
            "Alacak",
            money(db.totalReceivable())
        );

        addCard(
            "Gider",
            money(db.totalExpense())
        );

        addCard(
            "Net Kasa",
            money(
                db.totalCash() -
                db.totalExpense()
            )
        );

        content.addView(
            tv(
                "Sipariş durumları ve müşteri cari " +
                "hareketleri uygulama içinden takip edilir.",
                15
            )
        );
    }

    class DB extends SQLiteOpenHelper {

        DB(Context c) {
            super(
                c,
                "seyma_ofis.db",
                null,
                2
            );
        }

        public void onCreate(SQLiteDatabase x) {

            x.execSQL(
                "create table customers(" +
                "id integer primary key autoincrement," +
                "name text," +
                "phone text," +
                "address text," +
                "tax text," +
                "note text)"
            );

            x.execSQL(
                "create table orders(" +
                "id integer primary key autoincrement," +
                "customer_id integer," +
                "kind text," +
                "width integer," +
                "height integer," +
                "case_mm integer," +
                "opening text," +
                "lock text," +
                "material text," +
                "price real," +
                "paid real," +
                "status text," +
                "note text," +
                "created text)"
            );

            x.execSQL(
                "create table payments(" +
                "id integer primary key autoincrement," +
                "customer_id integer," +
                "amount real," +
                "method text," +
                "description text," +
                "created text)"
            );

            x.execSQL(
                "create table expenses(" +
                "id integer primary key autoincrement," +
                "title text," +
                "amount real," +
                "created text)"
            );
        }

        public void onUpgrade(
            SQLiteDatabase x,
            int a,
            int b
        ) {
        }

        void exec(String s,Object...p) {

            SQLiteStatement q =
                getWritableDatabase()
                .compileStatement(s);

            for(int i=0;i<p.length;i++) {

                Object v = p[i];

                if(v instanceof Number)
                    q.bindDouble(
                        i+1,
                        ((Number)v).doubleValue()
                    );
                else
                    q.bindString(
                        i+1,
                        String.valueOf(v)
                    );
            }

            q.executeInsert();
        }

        Cursor q(String s) {
            return getReadableDatabase()
                .rawQuery(s,null);
        }

        double totalSales() {

            Cursor c =
                q("select coalesce(sum(price),0) from orders");

            c.moveToFirst();

            double v = c.getDouble(0);

            c.close();

            return v;
        }

        double totalCash() {

            Cursor c =
                q("select coalesce(sum(amount),0) from payments");

            c.moveToFirst();

            double v = c.getDouble(0);

            c.close();

            return v;
        }

        double totalExpense() {

            Cursor c =
                q("select coalesce(sum(amount),0) from expenses");

            c.moveToFirst();

            double v = c.getDouble(0);

            c.close();

            return v;
        }

        double totalReceivable() {

            Cursor c =
                q(
                    "select coalesce(" +
                    "sum(price-paid),0) " +
                    "from orders"
                );

            c.moveToFirst();

            double v = c.getDouble(0);

            c.close();

            return v;
        }
    }
}
