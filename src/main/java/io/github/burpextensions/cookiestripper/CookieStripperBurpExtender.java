package io.github.burpextensions.cookiestripper;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.IHttpListener;
import burp.IHttpRequestResponse;
import burp.IInterceptedProxyMessage;
import burp.IProxyListener;
import burp.IRequestInfo;
import burp.ITab;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;

public class CookieStripperBurpExtender implements IBurpExtender, ITab, IHttpListener, IProxyListener {
    private static final String EXTENSION_NAME = "Cookie Stripper";
    private static final Set<String> COMMON_SESSION_COOKIES = new HashSet<String>(Arrays.asList(
            "jsessionid",
            "phpsessid",
            "asp.net_sessionid",
            "sessionid",
            "session",
            "sid",
            "connect.sid",
            "cfid",
            "cftoken"
    ));

    private final CookieInventoryTableModel tableModel = new CookieInventoryTableModel();

    private IBurpExtenderCallbacks callbacks;
    private JPanel rootPanel;
    private JLabel statusLabel;
    private JCheckBox stripProxyRequestsCheckbox;
    private JCheckBox learnProxyRequestsCheckbox;
    private JCheckBox learnNonProxyRequestsCheckbox;

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;

        callbacks.setExtensionName(EXTENSION_NAME);
        buildUi();
        callbacks.customizeUiComponent(rootPanel);
        callbacks.addSuiteTab(this);
        callbacks.registerHttpListener(this);
        callbacks.registerProxyListener(this);
        callbacks.printOutput(EXTENSION_NAME + " loaded");
        callbacks.printOutput("Mode: rewrite Proxy request directly through Proxy listener");
        updateStatusLabel();
    }

    @Override
    public String getTabCaption() {
        return EXTENSION_NAME;
    }

    @Override
    public java.awt.Component getUiComponent() {
        return rootPanel;
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        if (!messageIsRequest || toolFlag == IBurpExtenderCallbacks.TOOL_PROXY) {
            return;
        }

        byte[] request = messageInfo.getRequest();
        if (request == null || request.length == 0) {
            return;
        }

        IRequestInfo requestInfo = callbacks.getHelpers().analyzeRequest(request);
        List<CookieHeaderUtils.CookieToken> cookies = CookieHeaderUtils.extractCookiesFromRequestHeaders(requestInfo.getHeaders());
        if (cookies.isEmpty()) {
            return;
        }

        if (learnNonProxyRequestsCheckbox.isSelected()) {
            learnCookies(cookies, toolName(toolFlag));
        }
    }

    @Override
    public void processProxyMessage(boolean messageIsRequest, IInterceptedProxyMessage message) {
        if (!messageIsRequest) {
            return;
        }

        IHttpRequestResponse messageInfo = message.getMessageInfo();
        byte[] request = messageInfo.getRequest();
        if (request == null || request.length == 0) {
            return;
        }

        IRequestInfo requestInfo = callbacks.getHelpers().analyzeRequest(request);
        List<CookieHeaderUtils.CookieToken> cookies = CookieHeaderUtils.extractCookiesFromRequestHeaders(requestInfo.getHeaders());
        if (cookies.isEmpty()) {
            return;
        }

        if (learnProxyRequestsCheckbox.isSelected()) {
            learnCookies(cookies, "Proxy");
        }

        if (!stripProxyRequestsCheckbox.isSelected()) {
            return;
        }

        byte[] rewritten = CookieHeaderUtils.rewriteRequestCookies(request, callbacks.getHelpers(), tableModel);
        if (rewritten == null) {
            return;
        }

        messageInfo.setRequest(rewritten);
        rehookProxyMessage(message);
        callbacks.printOutput("Rewrote Proxy request Cookie header");
    }

    private void buildUi() {
        rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JPanel filterPanel = new JPanel(new BorderLayout(8, 0));

        stripProxyRequestsCheckbox = new JCheckBox("Strip in Proxy requests", true);
        learnProxyRequestsCheckbox = new JCheckBox("Learn from Proxy requests", true);
        learnNonProxyRequestsCheckbox = new JCheckBox("Learn from non-Proxy requests", true);

        controlsPanel.add(stripProxyRequestsCheckbox);
        controlsPanel.add(learnProxyRequestsCheckbox);
        controlsPanel.add(learnNonProxyRequestsCheckbox);

        JTextField filterField = new JTextField();
        filterField.setPreferredSize(new Dimension(280, 28));
        filterPanel.add(new JLabel("Filter cookies:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);

        JTable table = new JTable(tableModel);
        tableModel.addTableModelListener(event -> updateStatusLabel());
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMaxWidth(90);

        TableRowSorter<CookieInventoryTableModel> sorter = new TableRowSorter<CookieInventoryTableModel>(tableModel);
        table.setRowSorter(sorter);
        filterField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            String text = filterField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1));
            }
        }));

        JButton clearInventoryButton = new JButton("Clear Inventory");
        clearInventoryButton.addActionListener(event -> {
            tableModel.clearInventory();
            updateStatusLabel();
        });

        JButton uncheckAllButton = new JButton("Uncheck All");
        uncheckAllButton.addActionListener(event -> {
            tableModel.setAllSelected(false);
            updateStatusLabel();
        });

        JButton selectSessionButton = new JButton("Select Common Session Cookies");
        selectSessionButton.addActionListener(event -> {
            tableModel.selectCookies(COMMON_SESSION_COOKIES);
            updateStatusLabel();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(clearInventoryButton);
        buttonPanel.add(uncheckAllButton);
        buttonPanel.add(selectSessionButton);

        statusLabel = new JLabel();
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.add(buttonPanel, BorderLayout.WEST);
        footerPanel.add(statusLabel, BorderLayout.EAST);

        topPanel.add(controlsPanel, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.SOUTH);

        rootPanel.add(topPanel, BorderLayout.NORTH);
        rootPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        rootPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private void learnCookies(List<CookieHeaderUtils.CookieToken> cookies, String toolName) {
        for (CookieHeaderUtils.CookieToken cookie : cookies) {
            tableModel.recordCookie(cookie.getName(), toolName);
        }
        SwingUtilities.invokeLater(this::updateStatusLabel);
    }

    private void rehookProxyMessage(IInterceptedProxyMessage message) {
        int action = message.getInterceptAction();
        if (action == IInterceptedProxyMessage.ACTION_DO_INTERCEPT) {
            message.setInterceptAction(IInterceptedProxyMessage.ACTION_DO_INTERCEPT_AND_REHOOK);
            return;
        }
        if (action == IInterceptedProxyMessage.ACTION_DONT_INTERCEPT) {
            message.setInterceptAction(IInterceptedProxyMessage.ACTION_DONT_INTERCEPT_AND_REHOOK);
            return;
        }
        if (action == IInterceptedProxyMessage.ACTION_FOLLOW_RULES) {
            message.setInterceptAction(IInterceptedProxyMessage.ACTION_FOLLOW_RULES_AND_REHOOK);
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText("Unique cookies: " + tableModel.getUniqueCount()
                + " | Selected to strip: " + tableModel.getSelectedCount());
    }

    private String toolName(int toolFlag) {
        if (toolFlag == IBurpExtenderCallbacks.TOOL_PROXY) {
            return "Proxy";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_REPEATER) {
            return "Repeater";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_SCANNER) {
            return "Scanner";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_INTRUDER) {
            return "Intruder";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_EXTENDER) {
            return "Extender";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_TARGET) {
            return "Target";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_SPIDER) {
            return "Spider";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_SEQUENCER) {
            return "Sequencer";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_COMPARER) {
            return "Comparer";
        }
        if (toolFlag == IBurpExtenderCallbacks.TOOL_DECODER) {
            return "Decoder";
        }
        return "Other";
    }
}
