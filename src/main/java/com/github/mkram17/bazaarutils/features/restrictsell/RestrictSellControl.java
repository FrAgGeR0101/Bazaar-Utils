package com.github.mkram17.bazaarutils.features.restrictsell;

/**
 * Immutable description of a single “restrict-sell” rule.
 * <br>Designed for Forge 1.8.9 – no Lombok or external libs.
 */
public final class RestrictSellControl {

    /* ----------------------------------------------------------- */
    /*  core data                                                  */
    /* ----------------------------------------------------------- */

    private boolean           enabled = true;      // may be toggled in-game
    private final RestrictSell.Rule rule;          // rule type (PRICE / VOLUME / NAME)

    /*   numeric rules   */ private double amount; // upper limit (price / volume)
    /*   name   rules    */ private String name;   // blocked item-name

    /* ----------------------------------------------------------- */
    /*  constructors                                               */
    /* ----------------------------------------------------------- */

    /** PRICE or VOLUME rule */
    public RestrictSellControl(RestrictSell.Rule rule, double limit) {
        this.rule   = rule;
        this.amount = limit;
    }

    /** NAME rule */
    public RestrictSellControl(RestrictSell.Rule rule, String itemName) {
        this.rule = rule;
        this.name = itemName;
    }

    /* ----------------------------------------------------------- */
    /*  getters / setters                                          */
    /* ----------------------------------------------------------- */

    public boolean isEnabled()             { return enabled; }
    public void    setEnabled(boolean b)   { enabled = b;    }

    public RestrictSell.Rule getRule()     { return rule;    }

    public double getAmount()              { return amount;  }
    public void   setAmount(double v)      { amount = v;     }

    public String getName()                { return name;    }
    public void   setName(String n)        { name = n;       }
}
