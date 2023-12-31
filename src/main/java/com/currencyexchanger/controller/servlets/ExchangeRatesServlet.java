package com.currencyexchanger.controller.servlets;

import com.currencyexchanger.DTO.ErrorDTO;
import com.currencyexchanger.DTO.RequestExchangeRateDTO;
import com.currencyexchanger.controller.Validator;
import com.currencyexchanger.controller.exception.*;
import com.currencyexchanger.model.ExchangeRateModel;
import com.currencyexchanger.repository.JDBCRepsitory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;

@WebServlet(name = "exchangeRates", value = "/exchangeRates")
public class ExchangeRatesServlet extends BaseServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            List<ExchangeRateModel> list = JDBCRepsitory.readExchangeRates();
            printWriter.println(objectMapper.writeValueAsString(list));

        } catch (DatabaseException e) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            printWriter.println(objectMapper.writeValueAsString(new ErrorDTO(e.getMessage())));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String baseCurrencyCode = request.getParameter("baseCurrencyCode");
        String targetCurrencyCode = request.getParameter("targetCurrencyCode");
        String stringRate = request.getParameter("rate");

        try {
            Validator.validateParameters(baseCurrencyCode, targetCurrencyCode, stringRate);
            Validator.validateRateCode(baseCurrencyCode + targetCurrencyCode);

            BigDecimal rate = new BigDecimal(stringRate);
            RequestExchangeRateDTO requestDTO = new RequestExchangeRateDTO(baseCurrencyCode, targetCurrencyCode, rate);

            ExchangeRateModel exchangeRateModel = JDBCRepsitory.createExchangeRate(requestDTO)
                    .orElseThrow(DatabaseException::new);
            printWriter.println(objectMapper.writeValueAsString(exchangeRateModel));

        } catch (InvalidCurrencyCodeException | InvalidRateCodeException
                 | InvalidParametersException | NotFoundCurrencyException e) {
            response.setStatus(response.SC_BAD_REQUEST);
            printWriter.println(objectMapper.writeValueAsString(new ErrorDTO(e.getMessage())));

        } catch (FileAlreadyExistsException e) {
            response.setStatus(response.SC_CONFLICT);
            printWriter.println(objectMapper.writeValueAsString(new ErrorDTO(e.getMessage())));

        } catch (DatabaseException e) {
            response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
            printWriter.println(objectMapper.writeValueAsString(new ErrorDTO(e.getMessage())));
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        printWriter = resp.getWriter();
        super.service(req, resp);
    }
}
