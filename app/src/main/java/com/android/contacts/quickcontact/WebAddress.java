/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.quickcontact;

import static android.util.Patterns.GOOD_IRI_CHAR;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Below is a partial copy of {@link android.net.WebAddress}. The original author doesn't
 * believe this API is suitable for making public. He recommends we copy it.
 * <p>
 * Web Address Parser
 * <p>
 * This is called WebAddress, rather than URL or URI, because it
 * attempts to parse the stuff that a user will actually type into a
 * browser address widget.
 * <p>
 * Unlike java.net.uri, this parser will not choke on URIs missing
 * schemes.  It will only throw a ParseException if the input is
 * really hosed.
 * <p>
 * If given an https scheme but no port, fills in port
 */
public class WebAddress {

  static final int MATCH_GROUP_SCHEME = 1;
  static final int MATCH_GROUP_AUTHORITY = 2;
  static final int MATCH_GROUP_HOST = 3;
  static final int MATCH_GROUP_PORT = 4;
  static final int MATCH_GROUP_PATH = 5;
  static Pattern sAddressPattern = Pattern.compile(
    /* scheme    */ "(?:(http|https|file)\\:\\/\\/)?" +
      /* authority */ "(?:([-A-Za-z0-9$_.+!*'(),;?&=]+(?:\\:[-A-Za-z0-9$_.+!*'(),;?&=]+)?)@)?" +
      /* host      */ "([" + GOOD_IRI_CHAR + "%_-][" + GOOD_IRI_CHAR + "%_\\.-]*|\\[[0-9a-fA-F:\\.]+\\])?" +
      /* port      */ "(?:\\:([0-9]*))?" +
      /* path      */ "(\\/?[^#]*)?" +
      /* anchor    */ ".*", Pattern.CASE_INSENSITIVE);
  private String mScheme;
  private String mHost;
  private int mPort;
  private String mPath;
  private String mAuthInfo;

  /**
   * parses given uriString.
   */
  public WebAddress(String address) throws ParseException {
    if (address == null) {
      throw new NullPointerException();
    }

    mScheme = "";
    mHost = "";
    mPort = -1;
    mPath = "/";
    mAuthInfo = "";

    Matcher m = sAddressPattern.matcher(address);
    String t;
    if (m.matches()) {
      t = m.group(MATCH_GROUP_SCHEME);
      if (t != null) mScheme = t.toLowerCase(Locale.ROOT);
      t = m.group(MATCH_GROUP_AUTHORITY);
      if (t != null) mAuthInfo = t;
      t = m.group(MATCH_GROUP_HOST);
      if (t != null) mHost = t;
      t = m.group(MATCH_GROUP_PORT);
      if (t != null && t.length() > 0) {
        // The ':' character is not returned by the regex.
        try {
          mPort = Integer.parseInt(t);
        } catch (NumberFormatException ex) {
          throw new ParseException("Bad port");
        }
      }
      t = m.group(MATCH_GROUP_PATH);
      if (t != null && t.length() > 0) {
                /* handle busted myspace frontpage redirect with
                   missing initial "/" */
        if (t.charAt(0) == '/') {
          mPath = t;
        } else {
          mPath = "/" + t;
        }
      }

    } else {
      // nothing found... outa here
      throw new ParseException("Bad address");
    }

        /* Get port from scheme or scheme from port, if necessary and
           possible */
    if (mPort == 443 && mScheme.equals("")) {
      mScheme = "https";
    } else if (mPort == -1) {
      if (mScheme.equals("https"))
        mPort = 443;
      else
        mPort = 80; // default
    }
    if (mScheme.equals("")) mScheme = "http";
  }

  @Override
  public String toString() {
    String port = "";
    if ((mPort != 443 && mScheme.equals("https")) ||
      (mPort != 80 && mScheme.equals("http"))) {
      port = ":" + Integer.toString(mPort);
    }
    String authInfo = "";
    if (mAuthInfo.length() > 0) {
      authInfo = mAuthInfo + "@";
    }

    return mScheme + "://" + authInfo + mHost + port + mPath;
  }

  public class ParseException extends Exception {
    public String response;

    ParseException(String response) {
      this.response = response;
    }
  }
}
