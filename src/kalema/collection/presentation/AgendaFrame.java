package kalema.collection.presentation;

import kalema.collection.datastorage.AgendaRepository;
import kalema.collection.datastorage.AgendaRepository.Event;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;

/** Responsive French month view. All database operations run outside the Swing thread. */
public final class AgendaFrame extends JFrame {
    // Resolve the active Nimbus palette instead of imposing application colors.
    private static final Color INK = nimbusColor("text"),
        MUTED = nimbusColor("nimbusDisabledText"),
        BLUE = nimbusColor("nimbusBase"),
        BG = nimbusColor("control"),
        SURFACE = nimbusColor("nimbusLightBackground"),
        LINE = nimbusColor("nimbusBlueGrey"),
        SELECTION = nimbusColor("nimbusSelectionBackground"),
        SELECTED_TEXT = nimbusColor("textHighlightText");
    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final AgendaRepository repository = new AgendaRepository();
    private YearMonth month = YearMonth.now();
    private LocalDate selected = LocalDate.now();
    private List<Event> events = new ArrayList<>();
    private final JPanel grid = new JPanel(new GridLayout(6,7,1,1));
    private final JPanel details = new WidthPanel();
    private final JLabel heading = label("",28,INK), summary = label("",13,MUTED),
        dayTitle = label("",21,INK), status = label("Connexion…",12,MUTED);
    private final JTextField search = new JTextField(17);
    private final JButton add = button("+  Nouveau rendez-vous", true);
    private final JButton retry = button("Actualiser",false);
    private boolean connected, busy;
    private int generation;

    public AgendaFrame() {
        super("Agenda");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980,650)); setSize(1280,820);
        JPanel root = panel(new BorderLayout()); root.setBackground(BG); setContentPane(root);
        JPanel workspace = panel(new BorderLayout(0,22)); workspace.setBackground(BG);
        workspace.setBorder(new EmptyBorder(28,28,20,28)); root.add(workspace);
        JPanel top = panel(new BorderLayout()); top.setOpaque(false);
        JPanel titles = vertical(); titles.setOpaque(false);
        titles.add(label("VOTRE ESPACE PERSONNEL",11,MUTED)); titles.add(Box.createVerticalStrut(6));
        titles.add(label("Une journée bien organisée.",25,INK));
        top.add(titles); top.add(add,BorderLayout.EAST); add.addActionListener(e -> edit(null));
        workspace.add(top,BorderLayout.NORTH);
        JPanel body = panel(new BorderLayout(22,0)); body.setOpaque(false);
        JPanel calendar = panel(new BorderLayout(0,18)); calendar.setBorder(new EmptyBorder(20,20,20,20));
        JPanel nav = panel(new BorderLayout(8,8));
        JPanel monthText = vertical(); monthText.add(heading); monthText.add(summary); nav.add(monthText);
        JPanel controls = panel(new FlowLayout(FlowLayout.RIGHT,5,0));
        JButton prev=button("‹",false), today=button("Aujourd’hui",false), next=button("›",false);
        prev.setToolTipText("Mois précédent"); next.setToolTipText("Mois suivant");
        controls.add(prev); controls.add(today); controls.add(next); nav.add(controls,BorderLayout.EAST);
        prev.addActionListener(e -> move(-1)); next.addActionListener(e -> move(1));
        today.addActionListener(e -> { selected=LocalDate.now(); month=YearMonth.from(selected); reload(); });
        calendar.add(nav,BorderLayout.NORTH);
        JPanel days = panel(new BorderLayout(0,10)); JPanel weekdays=panel(new GridLayout(1,7));
        for(String d:new String[]{"LUN.","MAR.","MER.","JEU.","VEN.","SAM.","DIM."}) {
            JLabel l=label(d,11,MUTED); l.setHorizontalAlignment(SwingConstants.CENTER); weekdays.add(l);
        }
        days.add(weekdays,BorderLayout.NORTH); grid.setBackground(LINE); days.add(grid); calendar.add(days);
        body.add(calendar);
        JPanel side = panel(new BorderLayout(0,16)); side.setPreferredSize(new Dimension(290,0));
        side.setBorder(new EmptyBorder(20,18,20,18));
        JPanel sideTop=vertical(); sideTop.add(label("LE FIL DE VOTRE JOURNÉE",11,MUTED));
        sideTop.add(Box.createVerticalStrut(10)); sideTop.add(dayTitle); sideTop.add(Box.createVerticalStrut(18));
        sideTop.add(label("Rechercher dans ce mois",12,MUTED)); sideTop.add(Box.createVerticalStrut(5));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE,36)); search.setToolTipText("Titre, lieu ou notes");
        search.getAccessibleContext().setAccessibleName("Rechercher un rendez-vous"); sideTop.add(search);
        side.add(sideTop,BorderLayout.NORTH); details.setLayout(new BoxLayout(details,BoxLayout.Y_AXIS)); details.setBackground(SURFACE);
        JScrollPane scroll=new JScrollPane(details); scroll.setBorder(null); scroll.getViewport().setBackground(SURFACE); scroll.getVerticalScrollBar().setUnitIncrement(16); side.add(scroll);
        body.add(side,BorderLayout.EAST); workspace.add(body);
        JPanel footer=panel(new BorderLayout(12,0)); footer.setOpaque(false); footer.add(status); footer.add(retry,BorderLayout.EAST);
        retry.addActionListener(e -> reload()); workspace.add(footer,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e){ render(); }
            public void removeUpdate(DocumentEvent e){ render(); }
            public void changedUpdate(DocumentEvent e){ render(); }
        });
        getRootPane().registerKeyboardAction(e -> edit(null),KeyStroke.getKeyStroke(KeyEvent.VK_N,InputEvent.CTRL_DOWN_MASK),JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> reload(),KeyStroke.getKeyStroke(KeyEvent.VK_F5,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        setLocationRelativeTo(null); reload();
    }

    private void move(int count) {
        month=month.plusMonths(count); selected=month.atDay(Math.min(selected.getDayOfMonth(),month.lengthOfMonth())); reload();
    }
    private LocalDate firstCell() { return month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }
    private void reload() {
        final int request=++generation; final LocalDate first=firstCell();
        connected=false; events=new ArrayList<>(); status.setForeground(MUTED); status.setText("Chargement des rendez-vous…"); render();
        new SwingWorker<List<Event>,Void>() {
            protected List<Event> doInBackground() throws Exception { return repository.load(first,first.plusDays(41)); }
            protected void done() {
                if(request!=generation)return;
                try { events=get(); connected=true; status.setForeground(INK); status.setText("● MySQL connecté  ·  Vos rendez-vous sont à jour"); }
                catch(Exception ex) { connected=false; status.setForeground(INK); status.setText("MySQL indisponible · Vérifiez la configuration, puis cliquez sur Actualiser."); status.setToolTipText(cause(ex)); }
                render();
            }
        }.execute();
    }
    private List<Event> matching(LocalDate date) {
        List<Event> list=new ArrayList<>(); String q=search.getText().trim().toLowerCase(FR);
        for(Event e:events) if((date==null||e.date.equals(date)) &&
            (e.title+" "+Objects.toString(e.location,"")+" "+Objects.toString(e.notes,"")).toLowerCase(FR).contains(q)) list.add(e);
        return list;
    }
    private void render() {
        String text=month.format(DateTimeFormatter.ofPattern("MMMM uuuu",FR)); heading.setText(Character.toUpperCase(text.charAt(0))+text.substring(1));
        long count=events.stream().filter(e -> YearMonth.from(e.date).equals(month)).count();
        summary.setText(connected ? count+" rendez-vous ce mois-ci" : "Votre calendrier mensuel");
        add.setEnabled(connected&&!busy); retry.setEnabled(!busy);
        grid.removeAll(); LocalDate first=firstCell();
        for(int i=0;i<42;i++) {
            final LocalDate date=first.plusDays(i); List<Event> list=matching(date);
            boolean active=date.equals(selected), current=date.equals(LocalDate.now());
            JPanel cell=panel(new BorderLayout(0,4));
            cell.setBackground(active?SELECTION:date.getDayOfWeek().getValue()>5?BG:SURFACE);
            cell.setBorder(active?BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BLUE,2),new EmptyBorder(7,7,7,7)):new EmptyBorder(9,9,9,9));
            JButton number=button(String.valueOf(date.getDayOfMonth())+(current?" •":""),false);
            number.setMargin(new Insets(0,0,0,0)); number.setContentAreaFilled(false); number.setBorderPainted(false);
            number.setHorizontalAlignment(SwingConstants.LEFT);
            number.setForeground(active?SELECTED_TEXT:YearMonth.from(date).equals(month)?(current?BLUE:INK):MUTED);
            number.setToolTipText(date.format(DATE)); number.addActionListener(e -> select(date)); cell.add(number,BorderLayout.NORTH);
            if(!list.isEmpty()) {
                JLabel event=label(list.get(0).start+"  "+list.get(0).title,11,active?SELECTED_TEXT:BLUE);
                event.setToolTipText(list.get(0).title); cell.add(event);
                if(list.size()>1)cell.add(label("+ "+(list.size()-1)+" autre(s)",10,active?SELECTED_TEXT:MUTED),BorderLayout.SOUTH);
            }
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){ select(date); if(e.getClickCount()==2&&connected)edit(null); }});
            grid.add(cell);
        }
        dayTitle.setText(selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM",FR)));
        details.removeAll(); boolean searching=!search.getText().trim().isEmpty();
        List<Event> list=matching(searching?null:selected);
        if(searching)list.removeIf(e -> !YearMonth.from(e.date).equals(month));
        if(list.isEmpty()) {
            details.add(Box.createVerticalStrut(30)); details.add(label(connected?"Une pause dans votre agenda":"Connectez votre base MySQL",14,INK));
            details.add(Box.createVerticalStrut(10)); details.add(label(connected?"Ajoutez un rendez-vous pour commencer.":"Consultez le guide d’installation fourni.",11,MUTED));
        }
        for(Event e:list) {
            JPanel card=vertical(); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,3,1,0,BLUE),new EmptyBorder(12,12,14,8)));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE,210)); card.setAlignmentX(0);
            card.add(label((searching?e.date.format(DATE)+"  ·  ":"")+e.start+" — "+e.end,12,BLUE));
            card.add(Box.createVerticalStrut(8)); JLabel title=label(e.title,16,INK); title.setToolTipText(e.title); card.add(title);
            if(e.location!=null&&!e.location.isEmpty()){ JLabel l=label(e.location,12,MUTED); l.setToolTipText(e.location); card.add(l); }
            if(e.notes!=null&&!e.notes.isEmpty()) {
                JTextArea notes=new JTextArea(e.notes); notes.setLineWrap(true); notes.setWrapStyleWord(true); notes.setEditable(false);
                notes.setFont(new Font("Segoe UI",0,12)); notes.setForeground(MUTED); notes.setRows(2);
                notes.setBorder(null); notes.setOpaque(false); notes.setMaximumSize(new Dimension(Integer.MAX_VALUE,38)); card.add(notes);
            }
            JPanel actions=panel(new FlowLayout(FlowLayout.LEFT,0,5)); JButton modify=button("Modifier",false), del=button("Supprimer",false);
            modify.setEnabled(!busy); del.setEnabled(!busy); modify.addActionListener(a -> edit(e));
            del.addActionListener(a -> { if(JOptionPane.showConfirmDialog(this,"Supprimer ce rendez-vous ?","Suppression",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                mutate(() -> repository.delete(e.id),null,null); });
            actions.add(modify); actions.add(del); card.add(actions); details.add(card); details.add(Box.createVerticalStrut(12));
        }
        grid.revalidate(); grid.repaint(); details.revalidate(); details.repaint();
    }
    private void select(LocalDate date) {
        selected=date; if(!YearMonth.from(date).equals(month)){month=YearMonth.from(date);reload();}else render();
    }
    private void edit(Event original) {
        if(!connected||busy)return;
        JDialog dialog=new JDialog(this,original==null?"Nouveau rendez-vous":"Modifier le rendez-vous",true);
        JPanel form=vertical(); form.setBorder(new EmptyBorder(22,24,22,24));
        JTextField title=new JTextField(original==null?"":original.title,28), place=new JTextField(original==null?"":original.location),
            date=new JTextField((original==null?selected:original.date).format(DATE)),
            start=new JTextField(original==null?"09:00":original.start.toString()), end=new JTextField(original==null?"10:00":original.end.toString());
        JTextArea notes=new JTextArea(original==null?"":original.notes,4,28); notes.setLineWrap(true); notes.setWrapStyleWord(true);
        field(form,"Titre *",title); field(form,"Date * (jj/mm/aaaa)",date);
        JPanel times=panel(new GridLayout(1,2,12,0)); JPanel a=vertical(), b=vertical(); field(a,"Début * (hh:mm)",start); field(b,"Fin * (hh:mm)",end); times.add(a); times.add(b); form.add(times);
        field(form,"Lieu",place); field(form,"Notes",new JScrollPane(notes));
        JLabel error=label(" ",12,INK); form.add(error);
        JPanel buttons=panel(new FlowLayout(FlowLayout.RIGHT)); JButton cancel=button("Annuler",false), save=button("Enregistrer",true);
        cancel.addActionListener(e -> dialog.dispose()); buttons.add(cancel); buttons.add(save); form.add(buttons);
        save.addActionListener(ae -> {
            try {
                Event e=new Event(); e.id=original==null?0:original.id; e.title=title.getText().trim(); e.location=place.getText().trim(); e.notes=notes.getText().trim();
                e.date=LocalDate.parse(date.getText().trim(),DATE);
                if(!start.getText().matches("[0-2][0-9]:[0-5][0-9]")||!end.getText().matches("[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("Utilisez le format hh:mm.");
                e.start=LocalTime.parse(start.getText()); e.end=LocalTime.parse(end.getText());
                if(e.title.isEmpty()||e.title.length()>200)throw new IllegalArgumentException("Le titre doit contenir entre 1 et 200 caractères.");
                if(e.location.length()>255||e.notes.length()>10000)throw new IllegalArgumentException("Lieu : 255 caractères ; notes : 10 000 maximum.");
                if(!e.end.isAfter(e.start))throw new IllegalArgumentException("La fin doit être après le début, le même jour.");
                save.setEnabled(false); cancel.setEnabled(false); dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                mutate(() -> repository.save(e),dialog,save);
            } catch(DateTimeException ex){ error.setText("Date ou heure invalide. Vérifiez les formats indiqués."); }
              catch(IllegalArgumentException ex){ error.setText(ex.getMessage()); }
        });
        dialog.setContentPane(form); dialog.getRootPane().setDefaultButton(save); dialog.pack(); dialog.setMinimumSize(new Dimension(480,dialog.getHeight())); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private interface Operation { void run() throws Exception; }
    private void mutate(Operation operation,JDialog dialog,JButton save) {
        busy=true; render();
        new SwingWorker<Void,Void>() {
            protected Void doInBackground() throws Exception { operation.run(); return null; }
            protected void done() {
                busy=false;
                try { get(); if(dialog!=null)dialog.dispose(); reload(); }
                catch(Exception e){
                    if(save!=null) { save.setEnabled(true); enableButtons(dialog.getContentPane()); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); }
                    JOptionPane.showMessageDialog(dialog==null?AgendaFrame.this:dialog,cause(e),"Enregistrement impossible",JOptionPane.ERROR_MESSAGE); render();
                }
            }
        }.execute();
    }
    private static void enableButtons(Container c){ for(Component x:c.getComponents()){if(x instanceof JButton)x.setEnabled(true);if(x instanceof Container)enableButtons((Container)x);} }
    private static String cause(Throwable e){while(e.getCause()!=null)e=e.getCause();return Objects.toString(e.getMessage(),"Connexion impossible.");}
    private static void field(JPanel p,String name,JComponent input){p.add(label(name,12,MUTED));p.add(Box.createVerticalStrut(5));input.setAlignmentX(0);p.add(input);p.add(Box.createVerticalStrut(13));}
    private static Color nimbusColor(String key){return new Color(UIManager.getColor(key).getRGB());}
    private static JPanel panel(LayoutManager layout){JPanel p=new JPanel(layout);p.setBackground(SURFACE);return p;}
    private static JPanel vertical(){JPanel p=new JPanel(){
        protected void addImpl(Component c,Object constraints,int index){
            if(c instanceof JComponent)((JComponent)c).setAlignmentX(0);super.addImpl(c,constraints,index);
        }
    };p.setBackground(SURFACE);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setAlignmentX(0);return p;}
    private static final class WidthPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(250,450);}
        public int getScrollableUnitIncrement(Rectangle r,int axis,int direction){return 18;}
        public int getScrollableBlockIncrement(Rectangle r,int axis,int direction){return Math.max(18,r.height-18);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
    private static JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font("Segoe UI",size>=16?Font.BOLD:Font.PLAIN,size));l.setForeground(color);l.setAlignmentX(0);return l;}
    private static JButton button(String text,boolean primary){JButton b=new JButton(text);b.setFont(new Font("Segoe UI",primary?Font.BOLD:Font.PLAIN,13));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setMargin(new Insets(9,12,9,12));return b;}
}
