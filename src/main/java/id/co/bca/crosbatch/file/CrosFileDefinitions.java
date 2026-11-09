package id.co.bca.crosbatch.file;

import id.co.bca.crosbatch.entity.BranchDim;
import id.co.bca.crosbatch.entity.JaminanDebitur;
import id.co.bca.crosbatch.entity.MasterDebitur;
import id.co.bca.crosbatch.entity.MasterKomitmen;
import id.co.bca.crosbatch.entity.MasterPinjaman;
import id.co.bca.crosbatch.entity.Npl;
import id.co.bca.crosbatch.entity.PemegangSaham;
import id.co.bca.crosbatch.entity.RegionDim;
import id.co.bca.crosbatch.entity.RelasiJaminanFasilitas;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Column-to-field mapping and load mode for every CROS file. Columns are matched by header name. */
@Component
public class CrosFileDefinitions {

    private final Map<CrosFileType, FileDefinition<?>> definitions = new EnumMap<>(CrosFileType.class);

    public CrosFileDefinitions() {
        // Composite keys. The original keys were extended after checking the 112025 files: without the
        // extra columns, several real rows shared a key and only the last one was kept.
        definitions.put(CrosFileType.MASTER_DEBITUR, FileDefinition.upsert(MasterDebitur.class,
                CrosFileDefinitions::masterDebitur,
                e -> Arrays.asList(e.getCustLnCurrKey(), e.getKategoriDebitur(), e.getKodeKanwil(), e.getKodeKcu(),
                        e.getKategoriKredit())));
        definitions.put(CrosFileType.MASTER_PINJAMAN, FileDefinition.upsert(MasterPinjaman.class,
                CrosFileDefinitions::masterPinjaman,
                e -> Arrays.asList(e.getCustLnCurrKey(), e.getKodeKanwil(), e.getKodeKcu(),
                        e.getNoRekeningPinjaman(), e.getNoPinjaman())));
        definitions.put(CrosFileType.MASTER_KOMITMEN, FileDefinition.upsert(MasterKomitmen.class,
                CrosFileDefinitions::masterKomitmen,
                e -> Arrays.asList(e.getCustLnCurrKey(), e.getKodeKanwil(), e.getKodeKcu(),
                        e.getNoRekeningPinjaman(), e.getNoKomitmen())));
        definitions.put(CrosFileType.JAMINAN_DEBITUR, FileDefinition.upsert(JaminanDebitur.class,
                CrosFileDefinitions::jaminanDebitur,
                e -> Arrays.asList(e.getCustLnCurrKey(), e.getCollCurrKey())));
        definitions.put(CrosFileType.NPL, FileDefinition.snapshot(Npl.class, CrosFileDefinitions::npl));
        definitions.put(CrosFileType.PEMEGANG_SAHAM,
                FileDefinition.snapshot(PemegangSaham.class, CrosFileDefinitions::pemegangSaham));
        definitions.put(CrosFileType.REGION_DIM, FileDefinition.snapshot(RegionDim.class, CrosFileDefinitions::regionDim));
        definitions.put(CrosFileType.BRANCH_DIM, FileDefinition.snapshot(BranchDim.class, CrosFileDefinitions::branchDim));
        definitions.put(CrosFileType.RELASI_JAMINAN_FASILITAS,
                FileDefinition.snapshot(RelasiJaminanFasilitas.class, CrosFileDefinitions::relasiJaminanFasilitas));
    }

    public FileDefinition<?> get(CrosFileType type) {
        FileDefinition<?> definition = definitions.get(type);
        if (definition == null) {
            throw new IllegalStateException("no definition for " + type);
        }
        return definition;
    }

    static MasterDebitur masterDebitur(Row r) {
        MasterDebitur e = new MasterDebitur();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setCollectKey(r.lng("COLLECT_KEY"));
        e.setAcctRegionKey(r.lng("ACCT_REGION_KEY"));
        e.setAcctKcuKey(r.lng("ACCT_KCU_KEY"));
        e.setCin(r.str("CIN"));
        e.setNamaDebitur(r.str("NAMA_DEBITUR"));
        e.setTglMenjadiNasabah(r.date("TGL_MENJADI_NASABAH"));
        e.setTglMenjadiDebitur(r.date("TGL_MENJADI_DEBITUR"));
        e.setNoGrup(r.str("NO_GRUP"));
        e.setNamaGrup(r.str("NAMA_GRUP"));
        e.setSektorEkonomi(r.str("SEKTOR_EKONOMI"));
        e.setKategoriDebitur(r.str("KATEGORI_DEBITUR"));
        e.setKodeKanwil(r.str("KODE_KANWIL"));
        e.setKodeKcu(r.str("KODE_KCU"));
        e.setKategoriKredit(r.str("KATEGORI_KREDIT"));
        e.setFlagHb(r.integer("FLAG_HB"));
        e.setBalanceIdr(r.dec("BALANCE_IDR"));
        e.setHariTunggakan(r.integer("HARI_TUNGGAKAN"));
        return e;
    }

    static JaminanDebitur jaminanDebitur(Row r) {
        JaminanDebitur e = new JaminanDebitur();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setCollCurrKey(r.lng("COLL_CURR_KEY"));
        e.setCollCurrencyKey(r.lng("COLL_CURRENCY_KEY"));
        e.setNoCollateral(r.str("NO_COLLATERAL"));
        e.setTipeJaminan(r.str("TIPE_JAMINAN"));
        e.setMataUang(r.str("MATA_UANG"));
        e.setDeskripsiJaminan(r.str("DESKRIPSI_JAMINAN"));
        e.setNilaiPasarOcur(r.dec("NILAI_PASAR_OCUR"));
        e.setNilaiPasarIdr(r.dec("NILAI_PASAR_IDR"));
        e.setNilaiTaksasiOcur(r.dec("NILAI_TAKSASI_OCUR"));
        e.setNilaiTaksasiIdr(r.dec("NILAI_TAKSASI_IDR"));
        e.setNilaiHakTanggunganOcur(r.dec("NILAI_HAK_TANGGUNGAN_OCUR"));
        e.setNilaiHakTanggunganIdr(r.dec("NILAI_HAK_TANGGUNGAN_IDR"));
        e.setNilaiLikuidasiOcur(r.dec("NILAI_LIKUIDASI_OCUR"));
        e.setNilaiLikuidasiIdr(r.dec("NILAI_LIKUIDASI_IDR"));
        e.setSolidNonSolid(r.str("SOLID_NON_SOLID"));
        e.setTglBap(r.date("TGL_BAP"));
        return e;
    }

    static MasterPinjaman masterPinjaman(Row r) {
        MasterPinjaman e = new MasterPinjaman();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setAcctLnCurrKey(r.lng("ACCT_LN_CURR_KEY"));
        e.setCommCurrKey(r.lng("COMM_CURR_KEY"));
        e.setLnCurrKey(r.lng("LN_CURR_KEY"));
        e.setProductSubTypeKey(r.lng("PRODUCT_SUB_TYPE_KEY"));
        e.setFacLnSegKey(r.lng("FAC_LN_SEG_KEY"));
        e.setCollectKey(r.lng("COLLECT_KEY"));
        e.setAcctRegionKey(r.lng("ACCT_REGION_KEY"));
        e.setAcctKcuKey(r.lng("ACCT_KCU_KEY"));
        e.setKodeKanwil(r.str("KODE_KANWIL"));
        e.setKodeKcu(r.str("KODE_KCU"));
        e.setNoRekeningPinjaman(r.str("NO_REKENING_PINJAMAN"));
        e.setNoPinjaman(r.str("NO_PINJAMAN"));
        e.setTglBukaPinjaman(r.date("TGL_BUKA_PINJAMAN"));
        e.setTglTutupPinjaman(r.date("TGL_TUTUP_PINJAMAN"));
        e.setKategoriKredit(r.str("KATEGORI_KREDIT"));
        e.setTipeProduk(r.str("TIPE_PRODUK"));
        e.setKodeProduk(r.str("KODE_PRODUK"));
        e.setNamaProduk(r.str("NAMA_PRODUK"));
        e.setMataUang(r.str("MATA_UANG"));
        e.setBalanceOcur(r.dec("BALANCE_OCUR"));
        e.setBalanceIdr(r.dec("BALANCE_IDR"));
        e.setBungaOcur(r.dec("BUNGA_OCUR"));
        e.setBungaIdr(r.dec("BUNGA_IDR"));
        e.setDendaOcur(r.dec("DENDA_OCUR"));
        e.setDendaIdr(r.dec("DENDA_IDR"));
        e.setBalanceOcurHb(r.dec("BALANCE_OCUR_HB"));
        e.setBalanceIdrHb(r.dec("BALANCE_IDR_HB"));
        e.setBungaOcurHb(r.dec("BUNGA_OCUR_HB"));
        e.setBungaIdrHb(r.dec("BUNGA_IDR_HB"));
        e.setDendaOcurHb(r.dec("DENDA_OCUR_HB"));
        e.setDendaIdrHb(r.dec("DENDA_IDR_HB"));
        e.setTglHb(r.date("TGL_HB"));
        e.setBalanceOcurHt(r.dec("BALANCE_OCUR_HT"));
        e.setBalanceIdrHt(r.dec("BALANCE_IDR_HT"));
        e.setBungaOcurHt(r.dec("BUNGA_OCUR_HT"));
        e.setBungaIdrHt(r.dec("BUNGA_IDR_HT"));
        e.setDendaOcurHt(r.dec("DENDA_OCUR_HT"));
        e.setDendaIdrHt(r.dec("DENDA_IDR_HT"));
        e.setTglHt(r.date("TGL_HT"));
        e.setHariTunggakan(r.integer("HARI_TUNGGAKAN"));
        e.setIsRestrukturisasi(r.integer("IS_RESTRUKTURISASI"));
        e.setKodeRestrukturisasi(r.str("KODE_RESTRUKTURISASI"));
        e.setFlagHb(r.integer("FLAG_HB"));
        return e;
    }

    static MasterKomitmen masterKomitmen(Row r) {
        MasterKomitmen e = new MasterKomitmen();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setAcctLnCurrKey(r.lng("ACCT_LN_CURR_KEY"));
        e.setCommCurrKey(r.lng("COMM_CURR_KEY"));
        e.setProductSubTypeKey(r.lng("PRODUCT_SUB_TYPE_KEY"));
        e.setFacLnSegKey(r.lng("FAC_LN_SEG_KEY"));
        e.setCollectKey(r.lng("COLLECT_KEY"));
        e.setAcctRegionKey(r.lng("ACCT_REGION_KEY"));
        e.setAcctKcuKey(r.lng("ACCT_KCU_KEY"));
        e.setKodeKanwil(r.str("KODE_KANWIL"));
        e.setKodeKcu(r.str("KODE_KCU"));
        e.setNoRekeningPinjaman(r.str("NO_REKENING_PINJAMAN"));
        e.setNoKomitmen(r.str("NO_KOMITMEN"));
        e.setTglBukaKomitmen(r.date("TGL_BUKA_KOMITMEN"));
        e.setTglTutupKomitmen(r.date("TGL_TUTUP_KOMITMEN"));
        e.setKategoriKredit(r.str("KATEGORI_KREDIT"));
        e.setTipeProduk(r.str("TIPE_PRODUK"));
        e.setKodeProduk(r.str("KODE_PRODUK"));
        e.setNamaProduk(r.str("NAMA_PRODUK"));
        e.setMataUang(r.str("MATA_UANG"));
        e.setPlafondOcur(r.dec("PLAFOND_OCUR"));
        e.setPlafondIdr(r.dec("PLAFOND_IDR"));
        e.setBalanceOcur(r.dec("BALANCE_OCUR"));
        e.setBalanceIdr(r.dec("BALANCE_IDR"));
        e.setBungaOcur(r.dec("BUNGA_OCUR"));
        e.setBungaIdr(r.dec("BUNGA_IDR"));
        e.setDendaOcur(r.dec("DENDA_OCUR"));
        e.setDendaIdr(r.dec("DENDA_IDR"));
        e.setBalanceOcurHb(r.dec("BALANCE_OCUR_HB"));
        e.setBalanceIdrHb(r.dec("BALANCE_IDR_HB"));
        e.setBungaOcurHb(r.dec("BUNGA_OCUR_HB"));
        e.setBungaIdrHb(r.dec("BUNGA_IDR_HB"));
        e.setDendaOcurHb(r.dec("DENDA_OCUR_HB"));
        e.setDendaIdrHb(r.dec("DENDA_IDR_HB"));
        e.setTglHb(r.date("TGL_HB"));
        e.setBalanceOcurHt(r.dec("BALANCE_OCUR_HT"));
        e.setBalanceIdrHt(r.dec("BALANCE_IDR_HT"));
        e.setBungaOcurHt(r.dec("BUNGA_OCUR_HT"));
        e.setBungaIdrHt(r.dec("BUNGA_IDR_HT"));
        e.setDendaOcurHt(r.dec("DENDA_OCUR_HT"));
        e.setDendaIdrHt(r.dec("DENDA_IDR_HT"));
        e.setTglHt(r.date("TGL_HT"));
        e.setPpap(r.dec("PPAP"));
        e.setPosisiDataPpap(r.str("POSISI_DATA_PPAP"));
        e.setCkpn(r.dec("CKPN"));
        e.setPosisiDataCkpn(r.str("POSISI_DATA_CKPN"));
        e.setHariTunggakan(r.integer("HARI_TUNGGAKAN"));
        e.setIsRestrukturisasi(r.integer("IS_RESTRUKTURISASI"));
        e.setKodeRestrukturisasi(r.str("KODE_RESTRUKTURISASI"));
        e.setFlagHb(r.integer("FLAG_HB"));
        return e;
    }

    static Npl npl(Row r) {
        Npl e = new Npl();
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setAcctLnCurrKey(r.lng("ACCT_LN_CURR_KEY"));
        e.setCommCurrKey(r.lng("COMM_CURR_KEY"));
        e.setProductSubTypeKey(r.lng("PRODUCT_SUB_TYPE_KEY"));
        e.setFacLnSegKey(r.lng("FAC_LN_SEG_KEY"));
        e.setCollectKey(r.lng("COLLECT_KEY"));
        e.setCurrKey(r.lng("CURR_KEY"));
        e.setAcctRegionKey(r.lng("ACCT_REGION_KEY"));
        e.setAcctKcuKey(r.lng("ACCT_KCU_KEY"));
        e.setNoRekeningPinjaman(r.str("NO_REKENING_PINJAMAN"));
        e.setNoKomitmen(r.str("NO_KOMITMEN"));
        e.setTipeProduk(r.str("TIPE_PRODUK"));
        e.setKodeProduk(r.str("KODE_PRODUK"));
        e.setNamaProduk(r.str("NAMA_PRODUK"));
        e.setNplDate(r.date("NPL_DATE"));
        e.setNplBalanceOcur(r.dec("NPL_BALANCE_OCUR"));
        e.setNplBalanceIdr(r.dec("NPL_BALANCE_IDR"));
        e.setNplNotesIntReceivablesOcur(r.dec("NPL_NOTES_INT_RECEIVABLES_OCUR"));
        e.setNplNotesIntReceivablesIdr(r.dec("NPL_NOTES_INT_RECEIVABLES_IDR"));
        e.setNplCurrAssessedOcur(r.dec("NPL_CURR_ASSESSED_OCUR"));
        e.setNplCurrAssessedIdr(r.dec("NPL_CURR_ASSESSED_IDR"));
        e.setCol3Date(r.date("COL_3_DATE"));
        e.setCol3BalanceOcur(r.dec("COL_3_BALANCE_OCUR"));
        e.setCol3BalanceIdr(r.dec("COL_3_BALANCE_IDR"));
        e.setCol3NotesIntReceivablesOcur(r.dec("COL_3_NOTES_INT_RECEIVABLES_OCUR"));
        e.setCol3NotesIntReceivablesIdr(r.dec("COL_3_NOTES_INT_RECEIVABLES_IDR"));
        e.setCol3CurrAssessedOcur(r.dec("COL_3_CURR_ASSESSED_OCUR"));
        e.setCol3CurrAssessedIdr(r.dec("COL_3_CURR_ASSESSED_IDR"));
        e.setCol4Date(r.date("COL_4_DATE"));
        e.setCol4BalanceOcur(r.dec("COL_4_BALANCE_OCUR"));
        e.setCol4BalanceIdr(r.dec("COL_4_BALANCE_IDR"));
        e.setCol4NotesIntReceivablesOcur(r.dec("COL_4_NOTES_INT_RECEIVABLES_OCUR"));
        e.setCol4NotesIntReceivablesIdr(r.dec("COL_4_NOTES_INT_RECEIVABLES_IDR"));
        e.setCol4CurrAssessedOcur(r.dec("COL_4_CURR_ASSESSED_OCUR"));
        e.setCol4CurrAssessedIdr(r.dec("COL_4_CURR_ASSESSED_IDR"));
        e.setCol5Date(r.date("COL_5_DATE"));
        e.setCol5BalanceOcur(r.dec("COL_5_BALANCE_OCUR"));
        e.setCol5BalanceIdr(r.dec("COL_5_BALANCE_IDR"));
        e.setCol5NotesIntReceivablesOcur(r.dec("COL_5_NOTES_INT_RECEIVABLES_OCUR"));
        e.setCol5NotesIntReceivablesIdr(r.dec("COL_5_NOTES_INT_RECEIVABLES_IDR"));
        e.setCol5CurrAssessedOcur(r.dec("COL_5_CURR_ASSESSED_OCUR"));
        e.setCol5CurrAssessedIdr(r.dec("COL_5_CURR_ASSESSED_IDR"));
        return e;
    }

    static PemegangSaham pemegangSaham(Row r) {
        PemegangSaham e = new PemegangSaham();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setNamaPemegangSaham(r.str("NAMA_PEMEGANG_SAHAM"));
        e.setPengurusPemilikSahamInd(r.str("PENGURUS_PEMILIK_SAHAM_IND"));
        e.setOwnerShare(r.dec("OWNER_SHARE"));
        return e;
    }

    static RegionDim regionDim(Row r) {
        RegionDim e = new RegionDim();
        e.setRegionCdNew(r.str("REGION_CD_NEW"));
        e.setRegionKey(r.lng("REGION_KEY"));
        e.setRegionCd(r.str("REGION_CD"));
        e.setRegionShrtNm(r.str("REGION_SHRT_NM"));
        e.setRegionLngNm(r.str("REGION_LNG_NM"));
        e.setStrtEffDt(r.date("STRT_EFF_DT"));
        e.setStrtEndDt(r.date("STRT_END_DT"));
        e.setLastUpdateDt(r.date("LAST_UPDATE_DT"));
        e.setBusinessLoadDt(r.date("BUSINESS_LOAD_DT"));
        e.setSourceSystem(r.str("SOURCE_SYSTEM"));
        return e;
    }

    static BranchDim branchDim(Row r) {
        BranchDim e = new BranchDim();
        e.setBranchKey(r.lng("BRANCH_KEY"));
        e.setBranchCd(r.str("BRANCH_CD"));
        e.setBranchShrtNm(r.str("BRANCH_SHRT_NM"));
        e.setBranchName(r.str("BRANCH_NAME"));
        e.setBranchAddress1(r.str("BRANCH_ADDRESS1"));
        e.setBranchAddress2(r.str("BRANCH_ADDRESS2"));
        e.setJaboNonjabotabek(r.str("JABO_NONJABOTABEK"));
        e.setKcuCd(r.str("KCU_CD"));
        e.setCityKey(r.lng("CITY_KEY"));
        e.setRegionKey(r.lng("REGION_KEY"));
        e.setLastUpdateDt(r.date("LAST_UPDATE_DT"));
        e.setBusinessLoadDt(r.date("BUSINESS_LOAD_DT"));
        e.setStrtEffDt(r.date("STRT_EFF_DT"));
        e.setStrtEndDt(r.date("STRT_END_DT"));
        e.setSourceSystem(r.str("SOURCE_SYSTEM"));
        e.setPhone(r.str("PHONE"));
        e.setContactPerson(r.str("CONTACT_PERSON"));
        e.setRegionBatch(r.str("REGION_BATCH"));
        e.setGroupCity(r.str("GROUP_CITY"));
        e.setBranchClosedDt(r.date("BRANCH_CLOSED_DT"));
        e.setCkkCd(r.str("CKK_CD"));
        e.setReferralCd(r.str("REFERRAL_CD"));
        e.setRuangPrioritasInd(r.str("RUANG_PRIORITAS_IND"));
        e.setRegionKeyDkk(r.lng("REGION_KEY_DKK"));
        e.setTimezone(r.str("TIMEZONE"));
        e.setGeography(r.str("GEOGRAPHY"));
        e.setCentralKfkk(r.str("CENTRAL_KFKK"));
        e.setFlagBigCity(r.str("FLAG_BIG_CITY"));
        e.setCcJaboInd(r.str("CC_JABO_IND"));
        e.setCcRegionCd(r.str("CC_REGION_CD"));
        e.setCenterId(r.str("CENTER_ID"));
        e.setDepotId(r.str("DEPOT_ID"));
        e.setActvDt(r.date("ACTV_DT"));
        e.setStatus(r.str("STATUS"));
        e.setTripCd(r.str("TRIP_CD"));
        e.setCitVendor(r.str("CIT_VENDOR"));
        e.setGlRegion(r.str("GL_REGION"));
        e.setSoaCd(r.str("SOA_CD"));
        e.setClassId(r.str("CLASS_ID"));
        e.setSlaProfileId(r.str("SLA_PROFILE_ID"));
        e.setLatitude(r.dec("LATITUDE"));
        e.setLongitude(r.dec("LONGITUDE"));
        e.setFax(r.str("FAX"));
        e.setEmail(r.str("EMAIL"));
        e.setDati2Cd(r.str("DATI2_CD"));
        e.setConBranchCd(r.str("CON_BRANCH_CD"));
        return e;
    }

    static RelasiJaminanFasilitas relasiJaminanFasilitas(Row r) {
        RelasiJaminanFasilitas e = new RelasiJaminanFasilitas();
        e.setTglData(r.date("TGL_DATA"));
        e.setCustLnCurrKey(r.lng("CUST_LN_CURR_KEY"));
        e.setAcctLnCurrKey(r.lng("ACCT_LN_CURR_KEY"));
        e.setCommCurrKey(r.lng("COMM_CURR_KEY"));
        e.setCollCurrKey(r.lng("COLL_CURR_KEY"));
        return e;
    }
}
