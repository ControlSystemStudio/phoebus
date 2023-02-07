/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller implementing endpoints to retrieve help content. Logic is added to
 * support localization.
 */
@RestController
@RequestMapping("/help")
@SuppressWarnings("unused")
public class HelpResource {

    @Autowired
    private AcceptHeaderLocaleResolver acceptHeaderResolver;

    private final Logger logger = Logger.getLogger(HelpResource.class.getName());
    private static final String CONTENT_TYPE = "text/html;charset=UTF-8";

    /**
     * Locates the (static) help resource and returns it as a string.
     *
     * @param request {@link HttpServletRequest} used to determine language, if not explicitly set as query parameter.
     * @param lang    Optional language query parameter that - if specified and valid - will override Accept-Language from
     *                {#request}.
     * @param what    The resource "type".
     * @return The contents of the help resource, or <code>null</code> if the requested resource could not be found.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "{what}", produces = CONTENT_TYPE)
    public String getHelpContent(@RequestParam(name = "lang", required = false) String lang,
                                 @PathVariable String what,
                                 HttpServletRequest request) {
        String language = determineLang(lang, request);
        String content;
        logger.log(Level.INFO, "Requesting " + what + " for language=" + language);
        InputStream inputStream;
        try {
            inputStream = getClass().getResourceAsStream("/static/" + what + "_" + language + ".html");
            return readInputStream(inputStream);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to read " + what + " resource for language=" + language + ", defaulting to 'en'");
            try {
                inputStream = getClass().getResourceAsStream("/static/" + what + "_en.html");
                return readInputStream(inputStream);
            } catch (Exception ioException) {
                logger.log(Level.SEVERE, "Unable to read find resource " + what + "_en.html");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
    }

    /**
     * Determines the language from either specified request parameter or HTTP header Accept-Language
     * @param lang User specified request parameter
     * @param request {@link HttpServletRequest} from which the Accept-Language is read.
     * @return A string identifying a language. May default to en.
     */
    private String determineLang(String lang, HttpServletRequest request) {
        if (lang != null && !lang.isEmpty()) {
            return lang.toLowerCase();
        }
        Locale locale = acceptHeaderResolver.resolveLocale(request);
        if(locale == null){
            return "en";
        }
        return locale.getLanguage();
    }

    /**
     * Reads input stream to string
     * @param inputStream The non-null {@link InputStream} to read.
     * @return A {@link String} representation of the {@link InputStream} data
     * @throws IOException If the {@link InputStream} cannot be read.
     */
    private String readInputStream(InputStream inputStream) throws IOException{
        try{
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
        finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
    }
}
