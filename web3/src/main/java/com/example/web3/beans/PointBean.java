package com.example.web3.beans;

import com.example.web3.model.ResultRow;
import com.example.web3.service.AreaChecker;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class PointBean implements Serializable {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Double r = 2.0;
    private Double x = 0.0;
    private Double y = 0.0;

    private ResultsBean resultsBean;


    public void setResultsBean(ResultsBean resultsBean) {
        this.resultsBean = resultsBean;
    }


    public String submit() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (resultsBean == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "resultsBean не проинициализирован", null));
            return null;
        }

        if (!validate(r, x, y, fc)) {
            return null;
        }

        addResult(x, y, r, fc);
        return null;
    }


    public void submitFromCanvas() {
        FacesContext fc = FacesContext.getCurrentInstance();

        if (resultsBean == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "resultsBean не проинициализирован", null));
            return;
        }

        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();

        Double xVal = parse(params.get("x"));
        Double yVal = parse(params.get("y"));
        Double rVal = parse(params.get("r"));

        if (xVal == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Введите x числом", null));
            return;
        }
        if (yVal == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Введите y числом", null));
            return;
        }
        if (rVal == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Выберите корректное значение R", null));
            return;
        }

        if (!validate(rVal, xVal, yVal, fc)) {
            return;
        }

        addResult(xVal, yVal, rVal, fc);
    }

    private void addResult(double x, double y, double r, FacesContext fc) {
        long start = System.nanoTime();
        boolean hit = AreaChecker.isHit(x, y, r);
        double execMs = (System.nanoTime() - start) / 1_000_000.0;

        ResultRow row = new ResultRow(
                x, y, r,
                hit,
                LocalDateTime.now().format(TF),
                String.format(Locale.US, "%.3f ms", execMs)
        );

        resultsBean.add(row);

    }

    private boolean validate(Double r, Double x, Double y, FacesContext fc) {
        if (r == null || x == null || y == null) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "X, Y и R должны быть заданы", null));
            return false;
        }


        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(r)) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "X, Y и R должны быть корректными числами", null));
            return false;
        }


        if (!(r == 1.0 || r == 1.5 || r == 2.0 || r == 2.5 || r == 3.0)) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "R должен быть одним из значений: 1, 1.5, 2, 2.5, 3", null));
            return false;
        }

        return true;
    }

    private Double parse(String raw) {
        if (raw == null) return null;
        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getR() { return r; }
    public void setR(Double r) { this.r = r; }

    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }

    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
}
