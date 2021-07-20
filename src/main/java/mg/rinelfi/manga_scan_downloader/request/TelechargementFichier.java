/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mg.rinelfi.manga_scan_downloader.request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import mg.rinelfi.manga_scan_downloader.interfaces.Observer;

/**
 *
 * @author elier
 */
public class TelechargementFichier {

    private long taille;
    private final String lien_code, destination, entete, agent_utilisateur;
    private String lien_formate;
    private final int chiffre_chapitre, chiffre_page, chapitre, page;
    private int index_extension;
    private final boolean chapitre_formate, page_formatee;
    private boolean en_attente, en_cours, termine;
    private final String[] extensions;
    private Observer observer;
    private boolean fichier_telecharge;

    public TelechargementFichier(String lien_code, String destination, int chiffre_chapitre, int chiffre_page, int chapitre, int page, boolean chapitre_formate, boolean page_formatee, String[] extensions) {
        this.taille = 0l;
        this.lien_code = lien_code;
        this.lien_formate = null;
        this.destination = destination;
        this.entete = "GET";
        this.agent_utilisateur = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.128 Safari/537.36";
        this.chiffre_chapitre = chiffre_chapitre;
        this.chiffre_page = chiffre_page;
        this.chapitre = chapitre;
        this.page = page;
        this.index_extension = 0;
        this.chapitre_formate = chapitre_formate;
        this.page_formatee = page_formatee;
        this.extensions = extensions;
        this.charger_information();
    }

    public boolean is_en_attente() {
        return this.en_attente;
    }

    public boolean is_en_cours() {
        return this.en_cours;
    }

    public boolean is_termine() {
        return this.termine;
    }

    public boolean fichier_existe() {
        final int TAILLE_EXTENSION = this.extensions.length;
        boolean existe = false;
        this.index_extension = 0;
        while (this.index_extension < TAILLE_EXTENSION && !existe) {
            try {
                HttpURLConnection connexion = (HttpURLConnection) (new URL(this.lien_formate())).openConnection();
                connexion.setRequestMethod(this.entete);
                connexion.addRequestProperty("User-Agent", this.agent_utilisateur);
                if (connexion.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    existe = true;
                } else {
                    this.index_extension++;
                }

            } catch (ProtocolException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return existe;
    }

    public void charger_information() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) (new URL(this.lien_formate())).openConnection();
            connection.setRequestMethod(this.entete);
            connection.addRequestProperty("User-Agent", this.agent_utilisateur);
            connection.getInputStream();
            this.taille = connection.getContentLength();
        } catch (IOException ex) {
            this.taille = 0;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void telecharger() throws MalformedURLException, IOException {
        this.en_attente = false;
        final int TAILLE_TAMPON = 1024;
        byte[] tampon = new byte[TAILLE_TAMPON];
        HttpURLConnection connexion = (HttpURLConnection) (new URL(this.lien_formate())).openConnection();
        connexion.setRequestMethod(this.entete);
        connexion.addRequestProperty("User-Agent", this.agent_utilisateur);
        if (connexion.getResponseCode() == HttpURLConnection.HTTP_OK) {
            final File FICHIER_SORTANT = new File(this.destination + File.separatorChar + this.chapitre + File.separatorChar + this.page + "." + this.extensions[this.index_extension]);
            if (!FICHIER_SORTANT.getParentFile().exists()) {
                FICHIER_SORTANT.getParentFile().mkdirs();
            }
            try (BufferedInputStream flux_entrant = new BufferedInputStream(connexion.getInputStream())) {
                BufferedOutputStream flux_sortant = new BufferedOutputStream(new FileOutputStream(FICHIER_SORTANT));
                int byte_lues = 0;
                long taille_ecrites = 0;
                while ((byte_lues = flux_entrant.read(tampon, 0, TAILLE_TAMPON)) > 0) {
                    flux_sortant.write(tampon, 0, byte_lues);
                    flux_sortant.flush();
                    taille_ecrites += byte_lues;
                    this.en_cours = true;
                    this.termine = false;
                    this.observer.observation((int) ((float) taille_ecrites * 100 / (float) this.taille));
                }
                this.en_cours = false;
                this.termine = true;
                this.observer.observation(100);
                flux_sortant.close();
                flux_entrant.close();
            }
            connexion.disconnect();
        } else {
            throw new IOException("Aucun fichier à télécharger");
        }
    }

    public String lien_formate() {
        String remplacement_chapitre = String.valueOf(this.chapitre),
                remplacement_page = String.valueOf(this.page);
        if (this.chapitre_formate) {
            String chapitre_string = String.valueOf(this.chapitre);
            while (chapitre_string.length() < this.chiffre_chapitre) {
                chapitre_string = "0" + chapitre_string;
            }
            remplacement_chapitre = chapitre_string;
        }
        if (this.page_formatee) {
            String page_string = String.valueOf(this.page);
            while (page_string.length() < this.chiffre_page) {
                page_string = "0" + page_string;
            }
            remplacement_page = page_string;
        }
        this.lien_formate = this.lien_code.replaceAll("( )", "%20%")
                .replaceAll("\\{\\{chapitre\\}\\}", remplacement_chapitre)
                .replaceAll("\\{\\{page\\}\\}", remplacement_page)
                .replaceAll("\\{\\{ext\\}\\}", this.extensions[this.index_extension]);

        return this.lien_formate;
    }

    public String lien_formate(int index_prochain, int page_prochain) {
        String remplacement_chapitre = String.valueOf(this.chapitre),
                remplacement_page = String.valueOf(page_prochain);
        if (this.chapitre_formate) {
            String chapitre_string = String.valueOf(this.chapitre);
            while (chapitre_string.length() < this.chiffre_chapitre) {
                chapitre_string = "0" + chapitre_string;
            }
            remplacement_chapitre = chapitre_string;
        }
        if (this.page_formatee) {
            String page_string = String.valueOf(page_prochain);
            while (page_string.length() < this.chiffre_page) {
                page_string = "0" + page_string;
            }
            remplacement_page = page_string;
        }
        return this.lien_code.replaceAll("( )", "%20%")
                .replaceAll("\\{\\{chapitre\\}\\}", remplacement_chapitre)
                .replaceAll("\\{\\{page\\}\\}", remplacement_page)
                .replaceAll("\\{\\{ext\\}\\}", this.extensions[index_prochain]);
    }

    public boolean prochaine_page_disponible() {
        final int TAILLE_EXTENSION = this.extensions.length;
        boolean existe = false;
        for (int index = 0; index < TAILLE_EXTENSION && !existe; index++) {
            try {
                HttpURLConnection connexion = (HttpURLConnection) (new URL(this.lien_formate(index, this.page + 1))).openConnection();
                connexion.setRequestMethod(this.entete);
                connexion.addRequestProperty("User-Agent", this.agent_utilisateur);
                existe = connexion.getResponseCode() == HttpURLConnection.HTTP_OK;
                if (existe) {

                }
            } catch (ProtocolException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            } catch (MalformedURLException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(TelechargementFichier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return existe;
    }

    public String nom_du_fichier() {
        String remplacement_page = String.valueOf(this.page);
        if (this.page_formatee) {
            String page_string = String.valueOf(this.page);
            while (page_string.length() < this.chiffre_page) {
                page_string = "0" + page_string;
            }
            remplacement_page = page_string;
        }
        return remplacement_page + "." + this.extensions[this.index_extension];
    }

    public void ajouter_observateur_etat(Observer observer) {
        this.observer = observer;
    }

    public void telechargement_effectue(boolean fichier_telecharge) {
        this.fichier_telecharge = fichier_telecharge;
    }

    public boolean fichier_est_telecgarde() {
        return this.fichier_telecharge;
    }

    public boolean telechargement_est_effectue() {
        return false;
    }
}
